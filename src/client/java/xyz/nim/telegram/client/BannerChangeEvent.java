package xyz.nim.telegram.client;

public record BannerChangeEvent(
    int mapId,
    ChangeType changeType,
    String decorationKey,
    DecorationSnapshot decoration,
    DecorationSnapshot oldDecoration
) {
    public enum ChangeType {
        ADDED,
        REMOVED,
        CHANGED
    }
}
