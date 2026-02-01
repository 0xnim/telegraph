package xyz.nim.telegraph.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

import java.util.Optional;

public class WorldIdentifier {

    public static Optional<String> getCurrentWorldId() {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return Optional.empty();
        }

        if (client.hasSingleplayerServer() && client.getSingleplayerServer() != null) {
            String worldName = client.getSingleplayerServer().getWorldData().getLevelName();
            return Optional.of(sanitizeForPath(worldName));
        }

        ServerData serverInfo = client.getCurrentServer();
        if (serverInfo != null) {
            return Optional.of(sanitizeForPath(serverInfo.ip));
        }

        return Optional.empty();
    }

    public static boolean isInWorld() {
        Minecraft client = Minecraft.getInstance();
        return client != null && client.level != null && client.player != null;
    }

    private static String sanitizeForPath(String input) {
        if (input == null || input.isEmpty()) {
            return "unknown";
        }
        return input.replaceAll("[^a-zA-Z0-9._-]", "_")
                    .replaceAll("_+", "_")
                    .replaceAll("^_|_$", "");
    }
}
