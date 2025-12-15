package xyz.nim.telegraph.client.trade;

import com.google.gson.*;
import net.minecraft.client.MinecraftClient;
import xyz.nim.telegraph.client.WorldIdentifier;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class TradePersistence {
    private static final String CONFIG_DIR = "config/telegraph";
    private static final String TRADES_FILE = "trade_status.json";
    private final Gson gson;

    public TradePersistence() {
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    }

    private Path getConfigDir() {
        String worldId = WorldIdentifier.getCurrentWorldId().orElse(null);
        if (worldId == null) {
            return null;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return null;

        Path configPath = client.runDirectory.toPath()
            .resolve(CONFIG_DIR)
            .resolve("worlds")
            .resolve(worldId);
        try {
            Files.createDirectories(configPath);
        } catch (IOException e) {
            System.err.println("Failed to create config directory: " + e.getMessage());
        }
        return configPath;
    }
    
    public void saveTradeStatuses(List<TradeOffer> trades) {
        Path configDir = getConfigDir();
        if (configDir == null) return;
        
        Path tradesPath = configDir.resolve(TRADES_FILE);
        
        try {
            List<Map<String, Object>> serialized = new ArrayList<>();
            
            for (TradeOffer trade : trades) {
                Map<String, Object> tradeData = new HashMap<>();
                tradeData.put("channelId", trade.getChannelId());
                tradeData.put("originalMessage", trade.getOriginalMessage());
                tradeData.put("timestamp", trade.getTimestamp().toString());
                tradeData.put("status", trade.getStatus().name());
                
                serialized.add(tradeData);
            }
            
            String json = gson.toJson(serialized);
            Files.writeString(tradesPath, json);
            
        } catch (IOException e) {
            System.err.println("Failed to save trade statuses: " + e.getMessage());
        }
    }
    
    public Map<String, TradeStatus> loadTradeStatuses() {
        Path configDir = getConfigDir();
        if (configDir == null) return new HashMap<>();
        
        Path tradesPath = configDir.resolve(TRADES_FILE);
        
        if (!Files.exists(tradesPath)) {
            return new HashMap<>();
        }
        
        try {
            String json = Files.readString(tradesPath);
            JsonArray array = JsonParser.parseString(json).getAsJsonArray();
            
            Map<String, TradeStatus> statuses = new HashMap<>();
            
            for (JsonElement element : array) {
                JsonObject obj = element.getAsJsonObject();
                
                int channelId = obj.get("channelId").getAsInt();
                String originalMessage = obj.get("originalMessage").getAsString();
                String statusStr = obj.get("status").getAsString();
                
                String key = channelId + ":" + originalMessage;
                statuses.put(key, TradeStatus.valueOf(statusStr));
            }
            
            return statuses;
            
        } catch (IOException e) {
            System.err.println("Failed to load trade statuses: " + e.getMessage());
            return new HashMap<>();
        }
    }
    
    public static String getTradeKey(TradeOffer trade) {
        return trade.getChannelId() + ":" + trade.getOriginalMessage();
    }
}
