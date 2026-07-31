package com.phobia.levels.boosters;

public class ActiveBooster {

    private final BoosterType type;
    private double multiplier;
    private long expiresAt; // epoch millis

    public ActiveBooster(BoosterType type, double multiplier, long expiresAt) {
        this.type = type;
        this.multiplier = multiplier;
        this.expiresAt = expiresAt;
    }

    public BoosterType getType() { return type; }
    public double getMultiplier() { return multiplier; }
    public long getExpiresAt() { return expiresAt; }

    public boolean isActive() {
        return System.currentTimeMillis() < expiresAt;
    }

    public long getRemainingSeconds() {
        long remaining = (expiresAt - System.currentTimeMillis()) / 1000L;
        return Math.max(0, remaining);
    }

    /**
     * Extend behavior: adds the new duration onto whatever time is left,
     * and keeps the higher of the two multipliers (see design note above).
     */
    public void extend(double newMultiplier, int minutes) {
        long base = isActive() ? expiresAt : System.currentTimeMillis();
        this.expiresAt = base + (minutes * 60_000L);
        this.multiplier = Math.max(this.multiplier, newMultiplier);
    }

    // TYPE;multiplier;expiresAt
    public String serialize() {
        return type.name() + ";" + multiplier + ";" + expiresAt;
    }

    public static ActiveBooster deserialize(String raw) {
        if (raw == null) return null;
        String[] parts = raw.split(";");
        if (parts.length != 3) return null;
        try {
            BoosterType type = BoosterType.valueOf(parts[0]);
            double multiplier = Double.parseDouble(parts[1]);
            long expiresAt = Long.parseLong(parts[2]);
            return new ActiveBooster(type, multiplier, expiresAt);
        } catch (Exception e) {
            return null;
        }
    }
}