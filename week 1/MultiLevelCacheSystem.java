package com.sem4.assignments.week1;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class MultiLevelCacheSystem {
    private final LruMap<String, VideoData> l1;
    private final LruMap<String, VideoData> l2;
    private final Map<String, VideoData> l3 = new HashMap<>();
    private final Map<String, Integer> accessCount = new HashMap<>();

    private final int promoteThreshold;

    private long totalRequests;
    private long l1Hits;
    private long l2Hits;
    private long l3Hits;
    private long misses;
    private double totalLatencyMs;

    public MultiLevelCacheSystem(int l1Capacity, int l2Capacity, int promoteThreshold) {
        this.l1 = new LruMap<>(Math.max(1, l1Capacity));
        this.l2 = new LruMap<>(Math.max(1, l2Capacity));
        this.promoteThreshold = Math.max(1, promoteThreshold);
    }

    public synchronized void putInDatabase(VideoData video) {
        l3.put(video.videoId, video);
    }

    public synchronized CacheLookupResult getVideo(String videoId) {
        totalRequests++;

        VideoData video = l1.get(videoId);
        if (video != null) {
            l1Hits++;
            int count = increaseAccess(videoId);
            totalLatencyMs += 0.5;
            return new CacheLookupResult(video, "L1", 0.5, false, count);
        }

        video = l2.get(videoId);
        if (video != null) {
            l2Hits++;
            int count = increaseAccess(videoId);
            boolean promoted = maybePromote(videoId, video, count);
            totalLatencyMs += 5.0;
            return new CacheLookupResult(video, "L2", 5.0, promoted, count);
        }

        video = l3.get(videoId);
        if (video != null) {
            l3Hits++;
            l2.put(videoId, video);
            int count = increaseAccess(videoId);
            boolean promoted = maybePromote(videoId, video, count);
            totalLatencyMs += 150.0;
            return new CacheLookupResult(video, "L3", 150.0, promoted, count);
        }

        misses++;
        totalLatencyMs += 150.0;
        return new CacheLookupResult(null, "MISS", 150.0, false, 0);
    }

    public synchronized void invalidateVideo(String videoId) {
        l1.remove(videoId);
        l2.remove(videoId);
        l3.remove(videoId);
        accessCount.remove(videoId);
    }

    public synchronized CacheStatistics getStatistics() {
        double l1Rate = totalRequests == 0 ? 0.0 : (l1Hits * 100.0) / totalRequests;
        double l2Rate = totalRequests == 0 ? 0.0 : (l2Hits * 100.0) / totalRequests;
        double l3Rate = totalRequests == 0 ? 0.0 : (l3Hits * 100.0) / totalRequests;
        double overallRate = totalRequests == 0 ? 0.0 : ((l1Hits + l2Hits + l3Hits) * 100.0) / totalRequests;
        double avgLatency = totalRequests == 0 ? 0.0 : totalLatencyMs / totalRequests;

        return new CacheStatistics(l1Rate, l2Rate, l3Rate, overallRate, avgLatency,
                l1.size(), l2.size(), l3.size());
    }

    private int increaseAccess(String id) {
        int count = accessCount.getOrDefault(id, 0) + 1;
        accessCount.put(id, count);
        return count;
    }

    private boolean maybePromote(String id, VideoData video, int count) {
        if (count >= promoteThreshold) {
            l1.put(id, video);
            return true;
        }
        return false;
    }

    private static class LruMap<K, V> extends LinkedHashMap<K, V> {
        private final int capacity;

        LruMap(int capacity) {
            super(16, 0.75f, true);
            this.capacity = capacity;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > capacity;
        }
    }

    public static class VideoData {
        public final String videoId;
        public final String title;
        public final String payload;
        public final long updatedAtMillis;

        public VideoData(String videoId, String title, String payload, long updatedAtMillis) {
            this.videoId = videoId;
            this.title = title;
            this.payload = payload;
            this.updatedAtMillis = updatedAtMillis;
        }

        @Override
        public String toString() {
            return "VideoData{videoId='" + videoId + "', title='" + title + "'}";
        }
    }

    public static class CacheLookupResult {
        public final VideoData video;
        public final String level;
        public final double latencyMs;
        public final boolean promotedToL1;
        public final int accessCount;

        public CacheLookupResult(VideoData video, String level, double latencyMs, boolean promotedToL1, int accessCount) {
            this.video = video;
            this.level = level;
            this.latencyMs = latencyMs;
            this.promotedToL1 = promotedToL1;
            this.accessCount = accessCount;
        }

        @Override
        public String toString() {
            return "CacheLookupResult{video=" + video + ", level='" + level + "', latencyMs=" + latencyMs
                    + ", promotedToL1=" + promotedToL1 + ", accessCount=" + accessCount + "}";
        }
    }

    public static class CacheStatistics {
        public final double l1HitRate;
        public final double l2HitRate;
        public final double l3HitRate;
        public final double overallHitRate;
        public final double averageLatencyMs;
        public final int l1Size;
        public final int l2Size;
        public final int l3Size;

        public CacheStatistics(double l1HitRate, double l2HitRate, double l3HitRate,
                               double overallHitRate, double averageLatencyMs,
                               int l1Size, int l2Size, int l3Size) {
            this.l1HitRate = l1HitRate;
            this.l2HitRate = l2HitRate;
            this.l3HitRate = l3HitRate;
            this.overallHitRate = overallHitRate;
            this.averageLatencyMs = averageLatencyMs;
            this.l1Size = l1Size;
            this.l2Size = l2Size;
            this.l3Size = l3Size;
        }

        @Override
        public String toString() {
            return "CacheStatistics{l1HitRate=" + l1HitRate + ", l2HitRate=" + l2HitRate
                    + ", l3HitRate=" + l3HitRate + ", overallHitRate=" + overallHitRate
                    + ", averageLatencyMs=" + averageLatencyMs + ", l1Size=" + l1Size
                    + ", l2Size=" + l2Size + ", l3Size=" + l3Size + "}";
        }
    }
}
