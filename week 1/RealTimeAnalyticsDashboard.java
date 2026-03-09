package com.sem4.assignments.week1;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RealTimeAnalyticsDashboard {
    private final Map<String, Integer> pageViews = new HashMap<>();
    private final Map<String, Set<String>> uniqueUsersByPage = new HashMap<>();
    private final Map<String, Integer> sourceCount = new HashMap<>();
    private int totalEvents;

    public synchronized void processEvent(PageViewEvent event) {
        if (event == null || event.url == null || event.userId == null || event.source == null) {
            return;
        }

        pageViews.put(event.url, pageViews.getOrDefault(event.url, 0) + 1);

        uniqueUsersByPage.putIfAbsent(event.url, new HashSet<>());
        uniqueUsersByPage.get(event.url).add(event.userId);

        String source = event.source.toLowerCase();
        sourceCount.put(source, sourceCount.getOrDefault(source, 0) + 1);

        totalEvents++;
    }

    public synchronized DashboardSnapshot getDashboard() {
        List<TopPage> topPages = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : pageViews.entrySet()) {
            int unique = uniqueUsersByPage.getOrDefault(entry.getKey(), Set.of()).size();
            topPages.add(new TopPage(entry.getKey(), entry.getValue(), unique));
        }
        topPages.sort((a, b) -> Integer.compare(b.views, a.views));
        if (topPages.size() > 10) {
            topPages = new ArrayList<>(topPages.subList(0, 10));
        }

        int totalSourceVisits = 0;
        for (int value : sourceCount.values()) {
            totalSourceVisits += value;
        }

        List<SourceStat> sourceStats = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : sourceCount.entrySet()) {
            double percent = totalSourceVisits == 0 ? 0.0 : (entry.getValue() * 100.0) / totalSourceVisits;
            sourceStats.add(new SourceStat(entry.getKey(), entry.getValue(), percent));
        }
        sourceStats.sort((a, b) -> Double.compare(b.percentage, a.percentage));

        return new DashboardSnapshot(Instant.now(), totalEvents, topPages, sourceStats);
    }

    public static class PageViewEvent {
        public final String url;
        public final String userId;
        public final String source;

        public PageViewEvent(String url, String userId, String source) {
            this.url = url;
            this.userId = userId;
            this.source = source;
        }
    }

    public static class TopPage {
        public final String url;
        public final int views;
        public final int uniqueVisitors;

        public TopPage(String url, int views, int uniqueVisitors) {
            this.url = url;
            this.views = views;
            this.uniqueVisitors = uniqueVisitors;
        }

        @Override
        public String toString() {
            return "TopPage{url='" + url + "', views=" + views + ", uniqueVisitors=" + uniqueVisitors + "}";
        }
    }

    public static class SourceStat {
        public final String source;
        public final int count;
        public final double percentage;

        public SourceStat(String source, int count, double percentage) {
            this.source = source;
            this.count = count;
            this.percentage = percentage;
        }

        @Override
        public String toString() {
            return "SourceStat{source='" + source + "', count=" + count + ", percentage=" + percentage + "}";
        }
    }

    public static class DashboardSnapshot {
        public final Instant generatedAt;
        public final int totalEvents;
        public final List<TopPage> topPages;
        public final List<SourceStat> sourceStats;

        public DashboardSnapshot(Instant generatedAt, int totalEvents, List<TopPage> topPages, List<SourceStat> sourceStats) {
            this.generatedAt = generatedAt;
            this.totalEvents = totalEvents;
            this.topPages = topPages;
            this.sourceStats = sourceStats;
        }

        @Override
        public String toString() {
            return "DashboardSnapshot{generatedAt=" + generatedAt + ", totalEvents=" + totalEvents
                    + ", topPages=" + topPages + ", sourceStats=" + sourceStats + "}";
        }
    }
}
