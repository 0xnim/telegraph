package xyz.nim.telegraph.client.carnite.trade;

public enum TradeStatus {
    PENDING("Pending", 0xFFFFAA00),      // Yellow - waiting for response
    ACCEPTED("Accepted", 0xFF00AA00),    // Green - trade agreed
    REJECTED("Rejected", 0xFFAA0000),    // Red - declined
    COUNTERED("Countered", 0xFF0088FF),  // Blue - counter-offer made
    EXPIRED("Expired", 0xFF888888);      // Gray - timed out
    
    private final String displayName;
    private final int color;
    
    TradeStatus(String displayName, int color) {
        this.displayName = displayName;
        this.color = color;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public int getColor() {
        return color;
    }
    
    public boolean isActive() {
        return this == PENDING || this == COUNTERED;
    }
    
    public boolean isComplete() {
        return this == ACCEPTED || this == REJECTED || this == EXPIRED;
    }
}
