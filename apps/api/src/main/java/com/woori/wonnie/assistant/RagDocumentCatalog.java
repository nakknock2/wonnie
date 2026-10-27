package com.woori.wonnie.assistant;

import org.springframework.ai.document.Document;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** Keeps knowledge keys and whole Markdown sections together, including their tables. */
record RagDocumentCatalog(String instructions, List<Document> knowledge) {
    private static final List<String> INSTRUCTIONS = List.of(
            "01_system_instructions.md", "02_moment_matrix_guide.md",
            "03_tone_persona_guide.md", "05_examples.md");
    private static final String KNOWLEDGE = "04_knowledge_base.md";
    private static final Pattern HEADING = Pattern.compile("(?m)^## \\[([A-Z0-9-]+)]([^\\r\\n]*)");

    static RagDocumentCatalog load(ResourceLoader loader, String location) {
        if (!location.startsWith("classpath:") && !location.startsWith("file:")) {
            throw new IllegalArgumentException("RAG location must use classpath: or file:");
        }
        var root = location.endsWith("/") ? location : location + "/";
        var instructions = new StringBuilder();
        for (var name : INSTRUCTIONS) {
            instructions.append("\n\n--- 참고 지침: ").append(name).append(" ---\n")
                    .append(read(loader, root + name));
        }
        return new RagDocumentCatalog(instructions.toString(), sections(read(loader, root + KNOWLEDGE)));
    }

    static List<Document> sections(String markdown) {
        var matcher = HEADING.matcher(markdown);
        var starts = new ArrayList<Integer>();
        var keys = new ArrayList<String>();
        var titles = new ArrayList<String>();
        var unique = new HashSet<String>();
        while (matcher.find()) {
            var key = matcher.group(1);
            if (!unique.add(key)) throw new IllegalArgumentException("Duplicate RAG knowledge key: " + key);
            starts.add(matcher.start());
            keys.add(key);
            titles.add(matcher.group(2).strip());
        }
        if (keys.isEmpty()) throw new IllegalArgumentException("RAG knowledge has no ## [KEY] sections");
        var result = new ArrayList<Document>();
        for (int i = 0; i < keys.size(); i++) {
            int end = i + 1 < keys.size() ? starts.get(i + 1) : markdown.length();
            var text = markdown.substring(starts.get(i), end).strip();
            if (text.length() > 12000) throw new IllegalArgumentException("RAG section too large: " + keys.get(i));
            result.add(Document.builder().id(keys.get(i)).text(text).metadata(Map.of(
                    "source", KNOWLEDGE, "knowledge_key", keys.get(i), "title", titles.get(i))).build());
        }
        return List.copyOf(result);
    }

    private static String read(ResourceLoader loader, String path) {
        try (var stream = loader.getResource(path).getInputStream()) {
            var bytes = stream.readNBytes(200001);
            if (bytes.length > 200000) throw new IllegalArgumentException("RAG document too large: " + path);
            var text = new String(bytes, StandardCharsets.UTF_8);
            if (text.isBlank()) throw new IllegalArgumentException("Empty RAG document: " + path);
            return text;
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read RAG document: " + path, exception);
        }
    }
}
