package com.sem4.assignments.week1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PlagiarismDetector {
    private final int n;
    private final Map<String, Set<String>> ngramToDocs = new HashMap<>();
    private final Map<String, Set<String>> documentNgrams = new HashMap<>();

    public PlagiarismDetector(int n) {
        if (n < 2) {
            n = 2;
        }
        this.n = n;
    }

    public synchronized void indexDocument(String docId, String text) {
        Set<String> ngrams = extractNgrams(text);
        documentNgrams.put(docId, ngrams);

        for (String gram : ngrams) {
            ngramToDocs.putIfAbsent(gram, new HashSet<>());
            ngramToDocs.get(gram).add(docId);
        }
    }

    public synchronized AnalysisReport analyzeDocument(String docId, String text) {
        Set<String> ngrams = extractNgrams(text);
        Map<String, Integer> matchCount = new HashMap<>();

        for (String gram : ngrams) {
            Set<String> docs = ngramToDocs.getOrDefault(gram, Collections.emptySet());
            for (String otherDoc : docs) {
                if (!otherDoc.equals(docId)) {
                    matchCount.put(otherDoc, matchCount.getOrDefault(otherDoc, 0) + 1);
                }
            }
        }

        List<SimilarityResult> results = new ArrayList<>();
        for (Map.Entry<String, Integer> pair : matchCount.entrySet()) {
            String otherDoc = pair.getKey();
            int matches = pair.getValue();
            int otherSize = documentNgrams.getOrDefault(otherDoc, Collections.emptySet()).size();
            int base = Math.max(Math.max(ngrams.size(), otherSize), 1);
            double similarity = (matches * 100.0) / base;
            boolean suspicious = similarity >= 15.0;
            boolean plagiarism = similarity >= 60.0;
            results.add(new SimilarityResult(otherDoc, matches, similarity, suspicious, plagiarism));
        }

        results.sort((a, b) -> Double.compare(b.similarityPercent, a.similarityPercent));
        return new AnalysisReport(ngrams.size(), results);
    }

    private Set<String> extractNgrams(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptySet();
        }

        String cleaned = text.toLowerCase().replaceAll("[^a-z0-9\\s]", " ").trim();
        if (cleaned.isBlank()) {
            return Collections.emptySet();
        }

        String[] words = cleaned.split("\\s+");
        if (words.length < n) {
            return Collections.emptySet();
        }

        Set<String> grams = new HashSet<>();
        for (int i = 0; i <= words.length - n; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < n; j++) {
                if (j > 0) {
                    sb.append(' ');
                }
                sb.append(words[i + j]);
            }
            grams.add(sb.toString());
        }
        return grams;
    }

    public static class AnalysisReport {
        public final int extractedNgrams;
        public final List<SimilarityResult> matches;

        public AnalysisReport(int extractedNgrams, List<SimilarityResult> matches) {
            this.extractedNgrams = extractedNgrams;
            this.matches = matches;
        }

        @Override
        public String toString() {
            return "AnalysisReport{extractedNgrams=" + extractedNgrams + ", matches=" + matches + "}";
        }
    }

    public static class SimilarityResult {
        public final String documentId;
        public final int matchingNgrams;
        public final double similarityPercent;
        public final boolean suspicious;
        public final boolean plagiarismDetected;

        public SimilarityResult(String documentId, int matchingNgrams, double similarityPercent,
                                boolean suspicious, boolean plagiarismDetected) {
            this.documentId = documentId;
            this.matchingNgrams = matchingNgrams;
            this.similarityPercent = similarityPercent;
            this.suspicious = suspicious;
            this.plagiarismDetected = plagiarismDetected;
        }

        @Override
        public String toString() {
            return "SimilarityResult{documentId='" + documentId + "', matchingNgrams=" + matchingNgrams
                    + ", similarityPercent=" + similarityPercent + ", suspicious=" + suspicious
                    + ", plagiarismDetected=" + plagiarismDetected + "}";
        }
    }
}
