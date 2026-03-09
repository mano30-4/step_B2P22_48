package com.sem4.assignments.week1;

public class ParkingLotManager {
    private final int capacity;
    private final double hourlyRate;

    private final int[] status; // 0 empty, 1 occupied, 2 deleted
    private final String[] plateAtSpot;
    private final long[] entryTime;

    private final int[] hourlyEntryCount = new int[24];
    private int occupied;
    private long totalProbes;
    private long parkCount;

    public ParkingLotManager(int capacity, double hourlyRate) {
        if (capacity <= 0) {
            capacity = 1;
        }
        this.capacity = capacity;
        this.hourlyRate = hourlyRate;
        this.status = new int[capacity];
        this.plateAtSpot = new String[capacity];
        this.entryTime = new long[capacity];
    }

    public synchronized ParkResult parkVehicle(String licensePlate) {
        String plate = normalize(licensePlate);
        if (plate.isEmpty()) {
            return new ParkResult(false, -1, 0, "Invalid license plate");
        }
        if (findSpot(plate) != -1) {
            return new ParkResult(false, -1, 0, "Vehicle already parked");
        }

        int start = hash(plate);
        int firstDeleted = -1;

        for (int i = 0; i < capacity; i++) {
            int index = (start + i) % capacity;

            if (status[index] == 2 && firstDeleted == -1) {
                firstDeleted = index;
            }

            if (status[index] == 0) {
                int target = firstDeleted == -1 ? index : firstDeleted;
                occupy(target, plate, i);
                return new ParkResult(true, target, i, "Assigned spot #" + target);
            }
        }

        if (firstDeleted != -1) {
            occupy(firstDeleted, plate, capacity - 1);
            return new ParkResult(true, firstDeleted, capacity - 1, "Assigned spot #" + firstDeleted);
        }

        return new ParkResult(false, -1, capacity, "Parking lot full");
    }

    public synchronized ExitResult exitVehicle(String licensePlate) {
        String plate = normalize(licensePlate);
        int index = findSpot(plate);
        if (index == -1) {
            return new ExitResult(false, -1, 0, 0.0, "Vehicle not found");
        }

        long now = System.currentTimeMillis();
        long durationMs = Math.max(0, now - entryTime[index]);
        double hours = Math.max(0.25, durationMs / 3_600_000.0);
        double fee = hours * hourlyRate;

        status[index] = 2;
        plateAtSpot[index] = null;
        entryTime[index] = 0;
        occupied--;

        return new ExitResult(true, index, durationMs, fee, "Spot released");
    }

    public synchronized int findNearestAvailableSpot(int entrance) {
        if (occupied == capacity) {
            return -1;
        }

        int e = Math.floorMod(entrance, capacity);
        for (int d = 0; d < capacity; d++) {
            int right = (e + d) % capacity;
            if (status[right] == 0 || status[right] == 2) {
                return right;
            }

            int left = Math.floorMod(e - d, capacity);
            if (left != right && (status[left] == 0 || status[left] == 2)) {
                return left;
            }
        }
        return -1;
    }

    public synchronized ParkingStatistics getStatistics() {
        double occupancyPercent = (occupied * 100.0) / capacity;
        double avgProbes = parkCount == 0 ? 0.0 : (double) totalProbes / parkCount;

        int peakHour = 0;
        int peakCount = 0;
        for (int i = 0; i < 24; i++) {
            if (hourlyEntryCount[i] > peakCount) {
                peakCount = hourlyEntryCount[i];
                peakHour = i;
            }
        }

        String peak = String.format("%02d:00-%02d:00", peakHour, (peakHour + 1) % 24);
        return new ParkingStatistics(occupancyPercent, avgProbes, peak, occupied, capacity);
    }

    private void occupy(int index, String plate, int probes) {
        status[index] = 1;
        plateAtSpot[index] = plate;
        entryTime[index] = System.currentTimeMillis();
        occupied++;
        parkCount++;
        totalProbes += probes;

        int hour = (int) ((entryTime[index] / 3_600_000L) % 24);
        hourlyEntryCount[hour]++;
    }

    private int findSpot(String plate) {
        int start = hash(plate);
        for (int i = 0; i < capacity; i++) {
            int index = (start + i) % capacity;
            if (status[index] == 0) {
                return -1;
            }
            if (status[index] == 1 && plate.equals(plateAtSpot[index])) {
                return index;
            }
        }
        return -1;
    }

    private int hash(String plate) {
        return Math.floorMod(plate.hashCode(), capacity);
    }

    private String normalize(String plate) {
        if (plate == null) {
            return "";
        }
        return plate.trim().toUpperCase();
    }

    public static class ParkResult {
        public final boolean success;
        public final int spotNumber;
        public final int probes;
        public final String message;

        public ParkResult(boolean success, int spotNumber, int probes, String message) {
            this.success = success;
            this.spotNumber = spotNumber;
            this.probes = probes;
            this.message = message;
        }

        @Override
        public String toString() {
            return "ParkResult{success=" + success + ", spotNumber=" + spotNumber + ", probes=" + probes
                    + ", message='" + message + "'}";
        }
    }

    public static class ExitResult {
        public final boolean success;
        public final int spotNumber;
        public final long durationMillis;
        public final double fee;
        public final String message;

        public ExitResult(boolean success, int spotNumber, long durationMillis, double fee, String message) {
            this.success = success;
            this.spotNumber = spotNumber;
            this.durationMillis = durationMillis;
            this.fee = fee;
            this.message = message;
        }

        @Override
        public String toString() {
            return "ExitResult{success=" + success + ", spotNumber=" + spotNumber + ", durationMillis="
                    + durationMillis + ", fee=" + fee + ", message='" + message + "'}";
        }
    }

    public static class ParkingStatistics {
        public final double occupancyPercent;
        public final double averageProbes;
        public final String peakHour;
        public final int occupied;
        public final int capacity;

        public ParkingStatistics(double occupancyPercent, double averageProbes, String peakHour, int occupied, int capacity) {
            this.occupancyPercent = occupancyPercent;
            this.averageProbes = averageProbes;
            this.peakHour = peakHour;
            this.occupied = occupied;
            this.capacity = capacity;
        }

        @Override
        public String toString() {
            return "ParkingStatistics{occupancyPercent=" + occupancyPercent + ", averageProbes=" + averageProbes
                    + ", peakHour='" + peakHour + "', occupied=" + occupied + ", capacity=" + capacity + "}";
        }
    }
}
