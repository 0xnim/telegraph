package xyz.nim.telegraph.client;

public enum ChannelSortOption {
    RECENT_ACTIVITY("Recent Activity", "Most recently active first"),
    NAME_ASC("Name (A-Z)", "Alphabetical order"),
    NAME_DESC("Name (Z-A)", "Reverse alphabetical"),
    MAP_ID_ASC("Map ID ↑", "By map ID ascending"),
    MAP_ID_DESC("Map ID ↓", "By map ID descending"),
    MESSAGE_COUNT("Most Messages", "Highest message count first"),
    CREATION_DATE("Newest First", "Most recently created first");

    private final String label;
    private final String description;

    ChannelSortOption(String label, String description) {
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
