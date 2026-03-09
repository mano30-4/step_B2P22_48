package com.sem4.assignments.week1;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class DNSCacheWithTTL {
    private final int maxSize;
    private final UpstreamDnsResolver resolver;
    private final LinkedHashMap<String, DNSEntry> cache;

    private long hits;
    private long misses;
    private long totalLookups;
    private long totalLookupTimeNanos;

    public DNSCacheWithTTL(int maxSize, UpstreamDnsResolver resolver) {
        this.maxSize = Math.max(1, maxSize);
        this.resolver = resolver;
        this.cache = new LinkedHashMap<>(16, 0.75f, true);
    }

    public synchronized ResolveResult resolve(String domain) {
        long start = System.nanoTime();
        String key = normalize(domain);
        long now = System.currentTimeMillis();

        removeExpiredEntries(now);

        DNSEntry entry = cache.get(key);
        if (entry != null && entry.expiryMillis > now) {
            hits++;
            totalLookups++;
            long lookupNanos = System.nanoTime() - start;
            totalLookupTimeNanos += lookupNanos;
            long ttlLeft = Math.max(0, (entry.expiryMillis - now) / 1000);
            return new ResolveResult(key, entry.ipAddress, "HIT", ttlLeft, lookupNanos / 1_000_000.0);
        }

        misses++;
        ResolvedAddress upstream = resolver.resolve(key);
        long expiry = now + (upstream.ttlSeconds * 1000L);

        cache.put(key, new DNSEntry(upstream.ipAddress, expiry));
        trimIfNeeded();

        totalLookups++;
        long lookupNanos = System.nanoTime() - start;
        totalLookupTimeNanos += lookupNanos;
        return new ResolveResult(key, upstream.ipAddress, "MISS", upstream.ttlSeconds, lookupNanos / 1_000_000.0);
    }

    public synchronized CacheStats getCacheStats() {
        long total = hits + misses;
        double hitRate = total == 0 ? 0.0 : (hits * 100.0) / total;
        double avgLookupMs = totalLookups == 0 ? 0.0 : (totalLookupTimeNanos / 1_000_000.0) / totalLookups;
        return new CacheStats(hits, misses, hitRate, avgLookupMs, cache.size());
    }

    private void removeExpiredEntries(long now) {
        Iterator<Map.Entry<String, DNSEntry>> it = cache.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, DNSEntry> pair = it.next();
            if (pair.getValue().expiryMillis <= now) {
                it.remove();
            }
        }
    }

    private void trimIfNeeded() {
        while (cache.size() > maxSize) {
            String firstKey = cache.keySet().iterator().next();
            cache.remove(firstKey);
        }
    }

    private String normalize(String domain) {
        if (domain == null) {
            return "";
        }
        return domain.trim().toLowerCase();
    }

    private static class DNSEntry {
        String ipAddress;
        long expiryMillis;

        DNSEntry(String ipAddress, long expiryMillis) {
            this.ipAddress = ipAddress;
            this.expiryMillis = expiryMillis;
        }
    }

    public interface UpstreamDnsResolver {
        ResolvedAddress resolve(String domain);
    }

    public static class ResolvedAddress {
        public final String ipAddress;
        public final long ttlSeconds;

        public ResolvedAddress(String ipAddress, long ttlSeconds) {
            this.ipAddress = ipAddress;
            this.ttlSeconds = ttlSeconds;
        }
    }

    public static class ResolveResult {
        public final String domain;
        public final String ipAddress;
        public final String source;
        public final long ttlSeconds;
        public final double lookupTimeMillis;

        public ResolveResult(String domain, String ipAddress, String source, long ttlSeconds, double lookupTimeMillis) {
            this.domain = domain;
            this.ipAddress = ipAddress;
            this.source = source;
            this.ttlSeconds = ttlSeconds;
            this.lookupTimeMillis = lookupTimeMillis;
        }

        @Override
        public String toString() {
            return "ResolveResult{domain='" + domain + "', ipAddress='" + ipAddress + "', source='" + source
                    + "', ttlSeconds=" + ttlSeconds + ", lookupTimeMillis=" + lookupTimeMillis + "}";
        }
    }

    public static class CacheStats {
        public final long hits;
        public final long misses;
        public final double hitRatePercent;
        public final double averageLookupTimeMillis;
        public final int cacheSize;

        public CacheStats(long hits, long misses, double hitRatePercent, double averageLookupTimeMillis, int cacheSize) {
            this.hits = hits;
            this.misses = misses;
            this.hitRatePercent = hitRatePercent;
            this.averageLookupTimeMillis = averageLookupTimeMillis;
            this.cacheSize = cacheSize;
        }

        @Override
        public String toString() {
            return "CacheStats{hits=" + hits + ", misses=" + misses + ", hitRatePercent=" + hitRatePercent
                    + ", averageLookupTimeMillis=" + averageLookupTimeMillis + ", cacheSize=" + cacheSize + "}";
        }
    }
}
