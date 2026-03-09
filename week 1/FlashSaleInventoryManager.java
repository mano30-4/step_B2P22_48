package com.sem4.assignments.week1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class FlashSaleInventoryManager {
    private final Map<String, Integer> stockMap = new HashMap<>();
    private final Map<String, Queue<Long>> waitingListMap = new HashMap<>();

    public synchronized void addProduct(String productId, int initialStock) {
        if (initialStock < 0) {
            initialStock = 0;
        }
        stockMap.put(productId, stockMap.getOrDefault(productId, 0) + initialStock);
        waitingListMap.putIfAbsent(productId, new LinkedList<>());
    }

    public synchronized int checkStock(String productId) {
        return stockMap.getOrDefault(productId, 0);
    }

    public synchronized PurchaseResult purchaseItem(String productId, long userId) {
        if (!stockMap.containsKey(productId)) {
            return new PurchaseResult(false, 0, -1, "Product not found");
        }

        int currentStock = stockMap.get(productId);
        if (currentStock > 0) {
            stockMap.put(productId, currentStock - 1);
            return new PurchaseResult(true, currentStock - 1, -1, "Purchase successful");
        }

        Queue<Long> queue = waitingListMap.get(productId);
        queue.add(userId);
        int position = queue.size();
        return new PurchaseResult(false, 0, position, "Stock over. Added to waiting list");
    }

    public synchronized int restock(String productId, int quantity) {
        if (quantity <= 0) {
            return checkStock(productId);
        }
        int newStock = stockMap.getOrDefault(productId, 0) + quantity;
        stockMap.put(productId, newStock);
        waitingListMap.putIfAbsent(productId, new LinkedList<>());
        return newStock;
    }

    public synchronized List<Long> getWaitingListSnapshot(String productId) {
        Queue<Long> queue = waitingListMap.get(productId);
        if (queue == null) {
            return List.of();
        }
        return new ArrayList<>(queue);
    }

    public static class PurchaseResult {
        private final boolean success;
        private final int remainingStock;
        private final int waitingListPosition;
        private final String message;

        public PurchaseResult(boolean success, int remainingStock, int waitingListPosition, String message) {
            this.success = success;
            this.remainingStock = remainingStock;
            this.waitingListPosition = waitingListPosition;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public int getRemainingStock() {
            return remainingStock;
        }

        public int getWaitingListPosition() {
            return waitingListPosition;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return "PurchaseResult{success=" + success + ", remainingStock=" + remainingStock
                    + ", waitingListPosition=" + waitingListPosition + ", message='" + message + "'}";
        }
    }
}
