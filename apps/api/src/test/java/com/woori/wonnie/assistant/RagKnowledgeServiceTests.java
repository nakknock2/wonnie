package com.woori.wonnie.assistant;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RagKnowledgeServiceTests {
    @Test void loadsUpdatedGuidesAndKeepsKnowledgeKeysAndTables() {
        var catalog = RagDocumentCatalog.load(new DefaultResourceLoader(), "classpath:rag/");
        assertThat(catalog.instructions()).contains("01_system_instructions.md", "02_moment_matrix_guide.md",
                "03_tone_persona_guide.md", "05_examples.md", "R13", "RECEIVE_KKULMONEY");
        assertThat(catalog.instructions()).doesNotContain("00. 워니 AI 연동 가이드");
        assertThat(catalog.knowledge()).hasSize(24).extracting(Document::getId).doesNotHaveDuplicates();
        assertThat(catalog.knowledge()).filteredOn(d -> d.getId().equals("FAQ-WONNIE"))
                .singleElement().satisfies(d -> {
                    assertThat(d.getText()).contains("| 질문 |", "결제를 막거나 해지를 대신 하지는 않아요");
                    assertThat(d.getMetadata()).containsEntry("source", "04_knowledge_base.md");
                });
    }

    @Test void rejectsMissingOrDuplicateKnowledgeSections() {
        assertThatThrownBy(() -> RagDocumentCatalog.sections("# no keys"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RagDocumentCatalog.sections("## [KEY] 첫째\nA\n## [KEY] 둘째\nB"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Duplicate");
    }

    @Test void semanticSearchAddsRelevantSectionWithSourceAndReusesCorpusEmbeddings() {
        var model = new TestEmbeddingModel();
        var service = service(model, true, "classpath:rag/");
        // Reading packaged resources does not call OpenAI at server startup.
        assertThat(model.corpusCalls.get()).isZero();
        var first = service.retrieve("넷플릭스 멤버십 해지는 어디서 하나요?");
        assertThat(first).contains("kb:SVC-NETFLIX", "멤버십 해지", "kb:FAQ-WONNIE", "kb:REG-DATA-USE")
                .doesNotContain("kb:SVC-ICLOUD");
        assertThat(service.retrieve("넷플릭스 요금제 변경 방법" )).contains("kb:SVC-NETFLIX");
        assertThat(model.corpusCalls.get()).isEqualTo(1);
        assertThat(model.queryCalls.get()).isEqualTo(2);
    }

    @Test void disabledRagReadsNoDocumentsAndCallsNoEmbeddingApi() {
        var model = new TestEmbeddingModel();
        var service = service(model, false, "classpath:does-not-exist/");
        assertThat(service.instructions()).isEmpty();
        assertThat(service.retrieve("넷플릭스")).isEmpty();
        assertThat(model.corpusCalls.get() + model.queryCalls.get()).isZero();
    }

    @Test void failedIndexIsNotPublishedAndNextRequestCanRetry() {
        var model = new TestEmbeddingModel();
        model.fail = true;
        var service = service(model, true, "classpath:rag/");
        assertThatThrownBy(() -> service.retrieve("넷플릭스"))
                .isInstanceOf(IllegalStateException.class).hasMessage("test embedding failure");
        model.fail = false;
        assertThat(service.retrieve("넷플릭스")).contains("kb:SVC-NETFLIX");
        assertThat(model.corpusCalls.get()).isEqualTo(1);
    }

    @Test void missingConfiguredDocumentsFailClearly() {
        assertThatThrownBy(() -> service(new TestEmbeddingModel(), true, "classpath:missing-rag/"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Cannot read RAG document");
    }

    @SuppressWarnings("unchecked")
    private RagKnowledgeService service(EmbeddingModel model, boolean enabled, String location) {
        ObjectProvider<EmbeddingModel> provider = mock(ObjectProvider.class);
        when(provider.getObject()).thenReturn(model);
        return new RagKnowledgeService(new DefaultResourceLoader(), provider, enabled, location, 3, .3, 12000);
    }

    // Deterministic test-only vectors; production uses the configured OpenAI EmbeddingModel.
    private static class TestEmbeddingModel implements EmbeddingModel {
        final AtomicInteger corpusCalls = new AtomicInteger();
        final AtomicInteger queryCalls = new AtomicInteger();
        boolean fail;
        @Override public float[] embed(Document document) { return vector(document.getText()); }
        @Override public EmbeddingResponse call(EmbeddingRequest request) {
            if (fail) throw new IllegalStateException("test embedding failure");
            if (request.getInstructions().size() > 1) corpusCalls.incrementAndGet();
            else queryCalls.incrementAndGet();
            var results = new ArrayList<Embedding>();
            for (int i = 0; i < request.getInstructions().size(); i++) {
                results.add(new Embedding(vector(request.getInstructions().get(i)), i));
            }
            return new EmbeddingResponse(results);
        }
        private float[] vector(String text) {
            boolean netflix = text.startsWith("## [") ? text.startsWith("## [SVC-NETFLIX]") : text.contains("넷플릭스");
            return netflix ? new float[]{1, 0, 0} : new float[]{0, 0, 1};
        }
    }
}
