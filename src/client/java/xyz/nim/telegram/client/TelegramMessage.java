package xyz.nim.telegram.client;

import java.time.Instant;

public record TelegramMessage(
    int mapId,
    String content,
    ChangeType type,
    Instant timestamp,
    DecorationSnapshot decoration
) {
    public enum ChangeType {
        ADDED,
        REMOVED,
        CHANGED
    }
    
    public static TelegramMessage from(BannerChangeEvent event) {
        String content = switch (event.changeType()) {
            case ADDED -> (event.decoration().name() != null ? event.decoration().name() : "Banner") + " added";
            case REMOVED -> (event.oldDecoration().name() != null ? event.oldDecoration().name() : "Banner") + " removed";
            case CHANGED -> (event.decoration().name() != null ? event.decoration().name() : "Banner") + " moved";
        };
        
        return new TelegramMessage(
            event.mapId(),
            content,
            ChangeType.valueOf(event.changeType().name()),
            Instant.now(),
            event.decoration() != null ? event.decoration() : event.oldDecoration()
        );
    }
    
    public static TelegramMessage from(MapDecorationChangeEvent event) {
        DecorationSnapshot decoration = event.decoration() != null ? event.decoration() : event.oldDecoration();
        String decorationName = decoration.name() != null ? decoration.name() : decoration.type();
        
        String content = switch (event.changeType()) {
            case ADDED -> decorationName + " added at (" + String.format("%.1f", decoration.x()) + ", " + String.format("%.1f", decoration.z()) + ")";
            case REMOVED -> decorationName + " removed from (" + String.format("%.1f", decoration.x()) + ", " + String.format("%.1f", decoration.z()) + ")";
            case CHANGED -> decorationName + " changed";
        };
        
        return new TelegramMessage(
            event.mapId(),
            content,
            ChangeType.valueOf(event.changeType().name()),
            Instant.now(),
            decoration
        );
    }
}
