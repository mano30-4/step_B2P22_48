package com.sem4.assignments.week1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AutocompleteSystem {
    private final Map<String, Integer> frequencyMap = new HashMap<>();

    public synchronized void addQuery(String query) {
        updateFrequency(query, 1);
    }

    public synchronized void updateFrequency(String query, int value) {
        String text = normalize(query);
        if (text.isEmpty() || value <= 0) {
            return;
        }
        frequencyMap.put(text, frequencyMap.getOrDefault(text, 0) + value);
    }

    public synchronized List<Suggestion> search(String prefix, int topK) {
        List<Suggestion> ans = new ArrayList<>();
        if (topK <= 0) {
            return ans;
        }

        String key = normalize(prefix);

        for (Map.Entry<String, Integer> entry : frequencyMap.entrySet()) {
            if (entry.getKey().startsWith(key)) {
                ans.add(new Suggestion(entry.getKey(), entry.getValue()));
            }
        }

        ans.sort((a, b) -> {
            if (b.frequency != a.frequency) {
                return Integer.compare(b.frequency, a.frequency);
            }
            return a.query.compareTo(b.query);
        });

        if (ans.size() > topK) {
            ans = new ArrayList<>(ans.subList(0, topK));
        }

        if (ans.isEmpty()) {
            ans = typoSuggestions(key, topK);
        }

        return ans;
    }

    public synchronized int getFrequency(String query) {
        return frequencyMap.getOrDefault(normalize(query), 0);
    }

    private List<Suggestion> typoSuggestions(String input, int topK) {
        List<Suggestion> list = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : frequencyMap.entrySet()) {
            int dist = editDistance(input, entry.getKey());
            if (dist <= 2) {
                list.add(new Suggestion(entry.getKey(), entry.getValue()));
            }
        }

        list.sort((a, b) -> Integer.compare(b.frequency, a.frequency));
        if (list.size() > topK) {
            list = new ArrayList<>(list.subList(0, topK));
        }
        return list;
    }

    private int editDistance(String a, String b) {
        int n = a.length();
        int m = b.length();
        int[][] dp = new int[n + 1][m + 1];

        for (int i = 0; i <= n; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= m; j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                int insert = dp[i][j - 1] + 1;
                int delete = dp[i - 1][j] + 1;
                int replace = dp[i - 1][j - 1] + cost;
                dp[i][j] = Math.min(insert, Math.min(delete, replace));
            }
        }

        return dp[n][m];
    }

    private String normalize(String s) {
        if (s == null) {
            return "";
        }
        return s.trim().toLowerCase();
    }

    public static class Suggestion {
        public final String query;
        public final int frequency;

        public Suggestion(String query, int frequency) {
            this.query = query;
            this.frequency = frequency;
        }

        @Override
        public String toString() {
            return "Suggestion{query='" + query + "', frequency=" + frequency + "}";
        }
    }
}
