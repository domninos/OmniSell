package net.omni.sell.handlers;

public record ActiveBooster(int dbId, String islandUUID, SellBooster definition, long expiryTime, long cooldownEnd) {

    public boolean isExpired() {
        return expiryTime != -1 && System.currentTimeMillis() >= expiryTime;
    }

    public boolean isOnCooldown() {
        return cooldownEnd != -1 && System.currentTimeMillis() < cooldownEnd;
    }

    public long getRemainingCooldown() {
        if (cooldownEnd == -1) return 0;
        long remaining = cooldownEnd - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    public long getRemainingDuration() {
        if (expiryTime == -1) return -1;
        long remaining = expiryTime - System.currentTimeMillis();
        return Math.max(0, remaining);
    }
}
