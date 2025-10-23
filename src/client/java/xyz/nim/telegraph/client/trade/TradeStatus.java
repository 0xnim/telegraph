package xyz.nim.telegraph.client.trade;

public enum TradeStatus {
    OPEN("Open", 0xFFFFFF00),           // Yellow
    ACCEPTED("Accepted", 0xFF00FF00),    // Green
    DECLINED("Declined", 0xFFFF0000),    // Red
    COUNTER("Counter-offer", 0xFFFF8800), // Orange
    EXPIRED("Expired", 0xFF888888);      // Gray
    
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
}
