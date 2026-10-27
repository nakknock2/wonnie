package com.woori.wonnie.assistant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;

@Service
@Profile("openai")
public class RagKnowledgeService {
    private static final Logger log = LoggerFactory.getLogger(RagKnowledgeService.class);
    private final ObjectProvider<EmbeddingModel> embeddingModels;
    private final RagDocumentCatalog catalog;
    private final int topK;
    private final double threshold;
    private final int maxContextChars;
    private volatile SimpleVectorStore index;

    public RagKnowledgeService(ResourceLoader loader, ObjectProvider<EmbeddingModel> embeddingModels,
            @Value("${app.rag.enabled:false}") boolean enabled,
            @Value("${app.rag.location:classpath:rag/}") String location,
            @Value("${app.rag.top-k:3}") int topK,
            @Value("${app.rag.similarity-threshold:0.3}") double threshold,
            @Value("${app.rag.max-context-chars:12000}") int maxContextChars) {
        if (topK < 1 || topK > 10 || threshold < 0 || threshold > 1
                || maxContextChars < 1000 || maxContextChars > 30000) {
            throw new IllegalArgumentException("Invalid RAG retrieval limits");
        }
        this.embeddingModels = embeddingModels;
        this.catalog = enabled ? RagDocumentCatalog.load(loader, location) : new RagDocumentCatalog("", List.of());
        this.topK = topK;
        this.threshold = threshold;
        this.maxContextChars = maxContextChars;
        log.info("RAG documents loaded: enabled={}, knowledgeSections={}", enabled, catalog.knowledge().size());
    }

    public String instructions() {
        return catalog.instructions();
    }

    public String retrieve(String question) {
        if (catalog.knowledge().isEmpty()) return "";
        var found = vectorStore().similaritySearch(SearchRequest.builder().query(question)
                .topK(topK).similarityThreshold(threshold).build());
        var selected = new LinkedHashMap<String, Document>();
        // The two general CHAT references are small and apply even to questions without a close match.
        for (var key : List.of("FAQ-WONNIE", "REG-DATA-USE")) {
            catalog.knowledge().stream().filter(d -> key.equals(d.getId())).findFirst()
                    .ifPresent(d -> selected.put(key, d));
        }
        if (found != null) found.forEach(d -> selected.putIfAbsent(d.getId(), d));
        var context = new StringBuilder("아래는 답변용 참고 지식입니다. 현재 고객의 사실이나 실행 결과가 아닙니다.\n");
        var included = new java.util.ArrayList<String>();
        for (var document : selected.values()) {
            var section = "\n[출처: " + document.getMetadata().get("source")
                    + " / kb:" + document.getId() + "]\n" + document.getText() + "\n";
            if (context.length() + section.length() > maxContextChars) continue;
            context.append(section);
            included.add(document.getId());
        }
        log.info("RAG context selected: keys={}", included);
        return context.toString();
    }

    private SimpleVectorStore vectorStore() {
        var ready = index;
        if (ready != null) return ready;
        synchronized (this) {
            if (index != null) return index;
            var model = embeddingModels.getObject();
            var documents = catalog.knowledge();
            // SimpleVectorStore.add normally embeds one document per HTTP request.
            // Batch the static corpus once; query embeddings still go to the actual model.
            var vectors = model.embed(documents.stream().map(Document::getText).toList());
            if (vectors.size() != documents.size()) throw new IllegalStateException("RAG embedding count mismatch");
            var byId = new LinkedHashMap<String, float[]>();
            int dimension = -1;
            for (int i = 0; i < documents.size(); i++) {
                var vector = vectors.get(i);
                if (vector == null || vector.length == 0) throw new IllegalStateException("Empty RAG embedding");
                if (dimension == -1) dimension = vector.length;
                if (vector.length != dimension) throw new IllegalStateException("RAG embedding dimensions mismatch");
                double norm = 0;
                for (var value : vector) {
                    if (!Float.isFinite(value)) throw new IllegalStateException("Invalid RAG embedding");
                    norm += (double) value * value;
                }
                if (norm == 0) throw new IllegalStateException("Zero RAG embedding");
                byId.put(documents.get(i).getId(), vector);
            }
            final int indexedDimensions = dimension;
            EmbeddingModel searchModel = new EmbeddingModel() {
                @Override public EmbeddingResponse call(EmbeddingRequest request) { return model.call(request); }
                @Override public float[] embed(Document document) { return byId.get(document.getId()); }
                // Avoid the default dimensions() implementation issuing a probe embedding for every observation.
                @Override public int dimensions() { return indexedDimensions; }
            };
            var building = SimpleVectorStore.builder(searchModel).build();
            building.add(documents);
            index = building;
            log.info("RAG index ready: knowledgeSections={}", documents.size());
            return building;
        }
    }
}
