package xyz.nim.telegraph.client;

public enum ChannelCategory {
    ALL("All", "All channels"),
    CARNITE("Carnite", "Channels using Carnite protocol"),
    TELEGRAPH("Telegraph", "Channels using Telegraph protocol"),
    ACTIVE("Active", "Recent activity within 24 hours"),
    INACTIVE("Inactive", "No activity in 24+ hours"),
    NEW("New", "Newly created channels"),
    ARCHIVED("Archived", "Archived channels"),
    MUTED("Muted", "Notifications disabled"),
    EMPTY("Empty", "No messages"),
    LOW("Low", "1-10 messages"),
    MEDIUM("Medium", "11-50 messages"),
    HIGH("High", "50+ messages");

    private final String label;
    private final String description;

    ChannelCategory(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }
}
