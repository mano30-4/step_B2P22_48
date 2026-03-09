package com.sem4.assignments.week1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UsernameAvailabilityChecker {
    private final Map<String, Long> usernameToUserId = new HashMap<>();
    private final Map<String, Integer> attemptCountMap = new HashMap<>();

    public synchronized boolean registerUsername(String username, long userId) {
        String name = clean(username);
        if (name.isEmpty()) {
            return false;
        }
        if (usernameToUserId.containsKey(name)) {
            return false;
        }
        usernameToUserId.put(name, userId);
        return true;
    }

    public synchronized boolean checkAvailability(String username) {
        String name = clean(username);
        attemptCountMap.put(name, attemptCountMap.getOrDefault(name, 0) + 1);
        return !usernameToUserId.containsKey(name);
    }

    public synchronized List<String> suggestAlternatives(String username, int count) {
        List<String> result = new ArrayList<>();
        String base = clean(username);
        if (count <= 0 || base.isEmpty()) {
            return result;
        }

        if (!usernameToUserId.containsKey(base)) {
            result.add(base);
        }

        int i = 1;
        while (result.size() < count && i <= count * 20) {
            String option1 = base + i;
            String option2 = base + "_" + i;
            String option3 = base + "." + i;
            addIfFree(result, option1, count);
            addIfFree(result, option2, count);
            addIfFree(result, option3, count);
            i++;
        }

        return result;
    }

    public synchronized AttemptedUsername getMostAttempted() {
        String bestName = null;
        int bestCount = 0;

        for (Map.Entry<String, Integer> entry : attemptCountMap.entrySet()) {
            if (entry.getValue() > bestCount) {
                bestName = entry.getKey();
                bestCount = entry.getValue();
            }
        }

        if (bestName == null) {
            return null;
        }
        return new AttemptedUsername(bestName, bestCount);
    }

    public synchronized int getAttemptCount(String username) {
        return attemptCountMap.getOrDefault(clean(username), 0);
    }

    private void addIfFree(List<String> list, String candidate, int limit) {
        if (list.size() >= limit) {
            return;
        }
        if (!candidate.isEmpty() && !usernameToUserId.containsKey(candidate) && !list.contains(candidate)) {
            list.add(candidate);
        }
    }

    private String clean(String username) {
        if (username == null) {
            return "";
        }
        return username.trim().toLowerCase();
    }

    public static class AttemptedUsername {
        private final String username;
        private final int attempts;

        public AttemptedUsername(String username, int attempts) {
            this.username = username;
            this.attempts = attempts;
        }

        public String getUsername() {
            return username;
        }

        public int getAttempts() {
            return attempts;
        }

        @Override
        public String toString() {
            return "AttemptedUsername{username='" + username + "', attempts=" + attempts + "}";
        }
    }
}
