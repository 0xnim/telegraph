package xyz.nim.telegraph.client;

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
