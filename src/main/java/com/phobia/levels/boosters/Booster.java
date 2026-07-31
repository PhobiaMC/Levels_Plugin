package com.phobia.levels.boosters;

import java.util.UUID;

public class Booster {

    private final String id;
    private final BoosterType type;
    private final BoosterScope scope;
    private final double multiplier;
    private final int minutes;

    public Booster(BoosterType type, BoosterScope scope, double multiplier, int minutes) {
        this(UUID.randomUUID().toString().substring(0, 8), type, scope, multiplier, minutes);
    }

    public Booster(String id, BoosterType type, BoosterScope scope, double multiplier, int minutes) {
        this.id = id;
        this.type = type;
        this.scope = scope;
        this.multiplier = multiplier;
        this.minutes = minutes;
    }

    public String getId() { return id; }
    public BoosterType getType() { return type; }
    public BoosterScope getScope() { return scope; }
    public double getMultiplier() { return multiplier; }
    public int getMinutes() { return minutes; }

    // id;TYPE;SCOPE;multiplier;minutes
    public String serialize() {
        return id + ";" + type.name() + ";" + scope.name() + ";" + multiplier + ";" + minutes;
    }

    public static Booster deserialize(String raw) {
        if (raw == null) return null;
        String[] parts = raw.split(";");
        if (parts.length != 5) return null;
        try {
            String id = parts[0];
            BoosterType type = BoosterType.valueOf(parts[1]);
            BoosterScope scope = BoosterScope.valueOf(parts[2]);
            double multiplier = Double.parseDouble(parts[3]);
            int minutes = Integer.parseInt(parts[4]);
            return new Booster(id, type, scope, multiplier, minutes);
        } catch (Exception e) {
            return null;
        }
    }
}