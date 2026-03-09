package com.sem4.assignments.week1;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TransactionAnalyzer {
    public List<TransactionPair> findTwoSum(List<Transaction> transactions, long target) {
        Map<Long, List<Transaction>> seen = new HashMap<>();
        List<TransactionPair> pairs = new ArrayList<>();

        for (Transaction tx : transactions) {
            long need = target - tx.amount;
            List<Transaction> match = seen.getOrDefault(need, Collections.emptyList());
            for (Transaction old : match) {
                pairs.add(new TransactionPair(old.id, tx.id));
            }
            seen.putIfAbsent(tx.amount, new ArrayList<>());
            seen.get(tx.amount).add(tx);
        }

        return pairs;
    }

    public List<TransactionPair> findTwoSumWithinWindow(List<Transaction> transactions, long target, Duration window) {
        List<Transaction> sorted = new ArrayList<>(transactions);
        sorted.sort((a, b) -> a.timestamp.compareTo(b.timestamp));

        ArrayDeque<Transaction> queue = new ArrayDeque<>();
        Map<Long, List<Transaction>> active = new HashMap<>();
        List<TransactionPair> pairs = new ArrayList<>();

        for (Transaction current : sorted) {
            while (!queue.isEmpty()) {
                Transaction first = queue.peekFirst();
                Duration diff = Duration.between(first.timestamp, current.timestamp);
                if (diff.compareTo(window) <= 0) {
                    break;
                }

                queue.pollFirst();
                List<Transaction> bucket = active.get(first.amount);
                if (bucket != null) {
                    bucket.remove(first);
                    if (bucket.isEmpty()) {
                        active.remove(first.amount);
                    }
                }
            }

            long need = target - current.amount;
            for (Transaction old : active.getOrDefault(need, Collections.emptyList())) {
                pairs.add(new TransactionPair(old.id, current.id));
            }

            queue.addLast(current);
            active.putIfAbsent(current.amount, new ArrayList<>());
            active.get(current.amount).add(current);
        }

        return pairs;
    }

    public List<KSumMatch> findKSum(List<Transaction> transactions, int k, long target) {
        if (k < 2) {
            return List.of();
        }

        List<Transaction> list = new ArrayList<>(transactions);
        list.sort((a, b) -> Long.compare(a.amount, b.amount));

        List<KSumMatch> result = new ArrayList<>();
        dfs(list, 0, k, target, new ArrayList<>(), result);
        return result;
    }

    public List<DuplicateAlert> detectDuplicates(List<Transaction> transactions) {
        Map<String, List<Transaction>> map = new HashMap<>();

        for (Transaction tx : transactions) {
            String key = tx.amount + "|" + tx.merchant.toLowerCase();
            map.putIfAbsent(key, new ArrayList<>());
            map.get(key).add(tx);
        }

        List<DuplicateAlert> alerts = new ArrayList<>();
        for (List<Transaction> group : map.values()) {
            if (group.size() < 2) {
                continue;
            }

            Set<String> accounts = new HashSet<>();
            List<Long> ids = new ArrayList<>();
            for (Transaction tx : group) {
                accounts.add(tx.accountId);
                ids.add(tx.id);
            }

            if (accounts.size() > 1) {
                Transaction sample = group.get(0);
                alerts.add(new DuplicateAlert(sample.amount, sample.merchant, accounts, ids));
            }
        }

        return alerts;
    }

    private void dfs(List<Transaction> list, int index, int k, long target,
                     List<Transaction> picked, List<KSumMatch> ans) {
        if (k == 0) {
            if (target == 0) {
                List<Long> ids = new ArrayList<>();
                long sum = 0;
                for (Transaction tx : picked) {
                    ids.add(tx.id);
                    sum += tx.amount;
                }
                ans.add(new KSumMatch(ids, sum));
            }
            return;
        }

        if (index >= list.size()) {
            return;
        }

        for (int i = index; i < list.size(); i++) {
            Transaction tx = list.get(i);
            picked.add(tx);
            dfs(list, i + 1, k - 1, target - tx.amount, picked, ans);
            picked.remove(picked.size() - 1);
        }
    }

    public static class Transaction {
        public final long id;
        public final long amount;
        public final String merchant;
        public final String accountId;
        public final Instant timestamp;

        public Transaction(long id, long amount, String merchant, String accountId, Instant timestamp) {
            this.id = id;
            this.amount = amount;
            this.merchant = merchant;
            this.accountId = accountId;
            this.timestamp = timestamp;
        }
    }

    public static class TransactionPair {
        public final long firstId;
        public final long secondId;

        public TransactionPair(long firstId, long secondId) {
            this.firstId = firstId;
            this.secondId = secondId;
        }

        @Override
        public String toString() {
            return "TransactionPair{firstId=" + firstId + ", secondId=" + secondId + "}";
        }
    }

    public static class KSumMatch {
        public final List<Long> transactionIds;
        public final long totalAmount;

        public KSumMatch(List<Long> transactionIds, long totalAmount) {
            this.transactionIds = transactionIds;
            this.totalAmount = totalAmount;
        }

        @Override
        public String toString() {
            return "KSumMatch{transactionIds=" + transactionIds + ", totalAmount=" + totalAmount + "}";
        }
    }

    public static class DuplicateAlert {
        public final long amount;
        public final String merchant;
        public final Set<String> accountIds;
        public final List<Long> transactionIds;

        public DuplicateAlert(long amount, String merchant, Set<String> accountIds, List<Long> transactionIds) {
            this.amount = amount;
            this.merchant = merchant;
            this.accountIds = accountIds;
            this.transactionIds = transactionIds;
        }

        @Override
        public String toString() {
            return "DuplicateAlert{amount=" + amount + ", merchant='" + merchant + "', accountIds=" + accountIds
                    + ", transactionIds=" + transactionIds + "}";
        }
    }
}
