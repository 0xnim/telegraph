package xyz.nim.telegraph.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

import java.util.Optional;

public class WorldIdentifier {

    public static Optional<String> getCurrentWorldId() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return Optional.empty();
        }

        if (client.isIntegratedServerRunning() && client.getServer() != null) {
            String worldName = client.getServer().getSaveProperties().getLevelName();
            return Optional.of(sanitizeForPath(worldName));
        }

        ServerInfo serverInfo = client.getCurrentServerEntry();
        if (serverInfo != null) {
            return Optional.of(sanitizeForPath(serverInfo.address));
        }

        return Optional.empty();
    }

    public static boolean isInWorld() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client != null && client.world != null && client.player != null;
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
