package com.sem4.assignments.week1;

import java.util.HashMap;
import java.util.Map;

public class DistributedRateLimiter {
    private static final long ONE_HOUR_MS = 60L * 60L * 1000L;

    private final int limitPerHour;
    private final Map<String, TokenBucket> buckets = new HashMap<>();

    public DistributedRateLimiter(int limitPerHour) {
        if (limitPerHour <= 0) {
            limitPerHour = 1;
        }
        this.limitPerHour = limitPerHour;
    }

    public synchronized RateLimitResult checkRateLimit(String clientId) {
        String key = normalize(clientId);
        TokenBucket bucket = buckets.get(key);
        if (bucket == null) {
            bucket = new TokenBucket(limitPerHour);
            buckets.put(key, bucket);
        }
        return bucket.consumeOneToken();
    }

    public synchronized RateLimitStatus getRateLimitStatus(String clientId) {
        String key = normalize(clientId);
        TokenBucket bucket = buckets.get(key);
        if (bucket == null) {
            bucket = new TokenBucket(limitPerHour);
            buckets.put(key, bucket);
        }
        return bucket.getStatus();
    }

    private String normalize(String clientId) {
        if (clientId == null) {
            return "";
        }
        return clientId.trim();
    }

    private static class TokenBucket {
        private final int maxTokens;
        private final double refillRatePerMs;

        private double currentTokens;
        private long lastRefillMs;

        TokenBucket(int maxTokens) {
            this.maxTokens = maxTokens;
            this.currentTokens = maxTokens;
            this.lastRefillMs = System.currentTimeMillis();
            this.refillRatePerMs = maxTokens / (double) ONE_HOUR_MS;
        }

        RateLimitResult consumeOneToken() {
            refill();
            if (currentTokens >= 1.0) {
                currentTokens -= 1.0;
                int remaining = (int) Math.floor(currentTokens);
                return new RateLimitResult(true, remaining, 0, "Allowed");
            }

            double needed = 1.0 - currentTokens;
            long retrySeconds = (long) Math.ceil((needed / refillRatePerMs) / 1000.0);
            if (retrySeconds < 1) {
                retrySeconds = 1;
            }
            return new RateLimitResult(false, 0, retrySeconds, "Rate limit exceeded");
        }

        RateLimitStatus getStatus() {
            refill();
            int used = maxTokens - (int) Math.floor(currentTokens);
            if (used < 0) {
                used = 0;
            }
            long resetEpochSeconds = (System.currentTimeMillis() + (long) ((maxTokens - currentTokens) / refillRatePerMs)) / 1000L;
            return new RateLimitStatus(used, maxTokens, resetEpochSeconds);
        }

        private void refill() {
            long now = System.currentTimeMillis();
            long elapsed = now - lastRefillMs;
            if (elapsed > 0) {
                currentTokens = Math.min(maxTokens, currentTokens + (elapsed * refillRatePerMs));
                lastRefillMs = now;
            }
        }
    }

    public static class RateLimitResult {
        public final boolean allowed;
        public final int remainingRequests;
        public final long retryAfterSeconds;
        public final String message;

        public RateLimitResult(boolean allowed, int remainingRequests, long retryAfterSeconds, String message) {
            this.allowed = allowed;
            this.remainingRequests = remainingRequests;
            this.retryAfterSeconds = retryAfterSeconds;
            this.message = message;
        }

        @Override
        public String toString() {
            return "RateLimitResult{allowed=" + allowed + ", remainingRequests=" + remainingRequests
                    + ", retryAfterSeconds=" + retryAfterSeconds + ", message='" + message + "'}";
        }
    }

    public static class RateLimitStatus {
        public final int used;
        public final int limit;
        public final long resetEpochSeconds;

        public RateLimitStatus(int used, int limit, long resetEpochSeconds) {
            this.used = used;
            this.limit = limit;
            this.resetEpochSeconds = resetEpochSeconds;
        }

        @Override
        public String toString() {
            return "RateLimitStatus{used=" + used + ", limit=" + limit + ", resetEpochSeconds=" + resetEpochSeconds + "}";
        }
    }
}
