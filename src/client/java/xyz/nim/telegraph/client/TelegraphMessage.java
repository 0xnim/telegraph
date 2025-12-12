package xyz.nim.telegraph.client;

import java.time.Instant;

public record TelegraphMessage(
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
    
    public static TelegraphMessage from(MapDecorationChangeEvent event) {
        DecorationSnapshot decoration = event.decoration() != null ? event.decoration() : event.oldDecoration();
        String decorationName = decoration.name() != null ? decoration.name() : decoration.type();
        
        String content = switch (event.changeType()) {
            case ADDED -> decorationName + " added at (" + String.format("%.1f", decoration.x()) + ", " + String.format("%.1f", decoration.z()) + ")";
            case REMOVED -> decorationName + " removed from (" + String.format("%.1f", decoration.x()) + ", " + String.format("%.1f", decoration.z()) + ")";
            case CHANGED -> decorationName + " changed";
        };
        
        return new TelegraphMessage(
            event.mapId(),
            content,
            ChangeType.valueOf(event.changeType().name()),
            Instant.now(),
            decoration
        );
    }
}
