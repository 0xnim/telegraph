package xyz.nim.telegraph.client;

import com.google.gson.*;
import net.minecraft.client.MinecraftClient;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

public class PersistenceManager {
    private static final String CONFIG_DIR = "config/telegraph";
    private static final String SETTINGS_FILE = "channel_settings.json";
    private static final String MESSAGES_FILE = "messages.json";
    private final Gson gson;
    
    public PersistenceManager() {
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    }
    
    private Path getConfigDir() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return null;
        
        Path configPath = client.runDirectory.toPath().resolve(CONFIG_DIR);
        try {
            Files.createDirectories(configPath);
        } catch (IOException e) {
            System.err.println("Failed to create config directory: " + e.getMessage());
        }
        return configPath;
    }
    
    public void saveChannelSettings(TelegraphChannel channel) {
        Path configDir = getConfigDir();
        if (configDir == null) return;
        
        Path settingsPath = configDir.resolve(SETTINGS_FILE);
        
        try {
            Map<Integer, ChannelSettings> settings = channel.getAllSettings();
            Map<Integer, Map<String, Object>> serialized = new HashMap<>();
            
            for (Map.Entry<Integer, ChannelSettings> entry : settings.entrySet()) {
                Map<String, Object> settingData = new HashMap<>();
                ChannelSettings s = entry.getValue();
                
                settingData.put("customName", s.getCustomName());
                settingData.put("notificationsEnabled", s.isNotificationsEnabled());
                settingData.put("archived", s.isArchived());
                settingData.put("tags", s.getTags());
                settingData.put("notificationLevel", s.getNotificationLevel().name());
                
                serialized.put(entry.getKey(), settingData);
            }
            
            Map<Integer, String> userNames = new HashMap<>();
            for (int mapId : channel.getAllChannelIds()) {
                channel.getChannelName(mapId).ifPresent(name -> userNames.put(mapId, name));
            }
            
            Map<String, Object> data = new HashMap<>();
            data.put("settings", serialized);
            data.put("userNames", userNames);
            
            String json = gson.toJson(data);
            Files.writeString(settingsPath, json);
            
        } catch (IOException e) {
            System.err.println("Failed to save channel settings: " + e.getMessage());
        }
    }
    
    public void loadChannelSettings(TelegraphChannel channel) {
        Path configDir = getConfigDir();
        if (configDir == null) return;
        
        Path settingsPath = configDir.resolve(SETTINGS_FILE);
        if (!Files.exists(settingsPath)) return;
        
        try {
            String json = Files.readString(settingsPath);
            JsonObject root = gson.fromJson(json, JsonObject.class);
            
            if (root.has("settings")) {
                JsonObject settingsObj = root.getAsJsonObject("settings");
                
                for (Map.Entry<String, JsonElement> entry : settingsObj.entrySet()) {
                    int mapId = Integer.parseInt(entry.getKey());
                    JsonObject settingData = entry.getValue().getAsJsonObject();
                    
                    ChannelSettings settings = channel.getOrCreateSettings(mapId);
                    
                    if (settingData.has("customName") && !settingData.get("customName").isJsonNull()) {
                        settings.setCustomName(settingData.get("customName").getAsString());
                    }
                    if (settingData.has("notificationsEnabled")) {
                        settings.setNotificationsEnabled(settingData.get("notificationsEnabled").getAsBoolean());
                    }
                    if (settingData.has("archived")) {
                        settings.setArchived(settingData.get("archived").getAsBoolean());
                    }
                    if (settingData.has("tags")) {
                        JsonArray tagsArray = settingData.getAsJsonArray("tags");
                        for (JsonElement tag : tagsArray) {
                            settings.addTag(tag.getAsString());
                        }
                    }
                    if (settingData.has("notificationLevel")) {
                        settings.setNotificationLevel(
                            ChannelSettings.NotificationLevel.valueOf(settingData.get("notificationLevel").getAsString())
                        );
                    }
                }
            }
            
            if (root.has("userNames")) {
                JsonObject userNamesObj = root.getAsJsonObject("userNames");
                for (Map.Entry<String, JsonElement> entry : userNamesObj.entrySet()) {
                    int mapId = Integer.parseInt(entry.getKey());
                    String name = entry.getValue().getAsString();
                    channel.setUserChannelName(mapId, name);
                }
            }
            
        } catch (IOException e) {
            System.err.println("Failed to load channel settings: " + e.getMessage());
        }
    }
    
    public void saveMessages(TelegraphChannel channel) {
        Path configDir = getConfigDir();
        if (configDir == null) return;
        
        Path messagesPath = configDir.resolve(MESSAGES_FILE);
        
        try {
            Map<Integer, List<Map<String, Object>>> serialized = new HashMap<>();
            
            for (int mapId : channel.getAllChannelIds()) {
                List<TelegraphMessage> messages = channel.getMessages(mapId);
                List<Map<String, Object>> messageData = new ArrayList<>();
                
                for (TelegraphMessage msg : messages) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("content", msg.content());
                    data.put("type", msg.type() != null ? msg.type().name() : null);
                    data.put("timestamp", msg.timestamp().toEpochMilli());
                    
                    if (msg.decoration() != null) {
                        Map<String, Object> decoData = new HashMap<>();
                        decoData.put("type", msg.decoration().type());
                        decoData.put("x", msg.decoration().x());
                        decoData.put("z", msg.decoration().z());
                        decoData.put("rotation", msg.decoration().rotation());
                        decoData.put("name", msg.decoration().name());
                        data.put("decoration", decoData);
                    }
                    
                    messageData.add(data);
                }
                
                serialized.put(mapId, messageData);
            }
            
            String json = gson.toJson(serialized);
            Files.writeString(messagesPath, json);
            
        } catch (IOException e) {
            System.err.println("Failed to save messages: " + e.getMessage());
        }
    }
    
    public void exportChannelData(int mapId, TelegraphChannel channel, Path exportPath) {
        try {
            Map<String, Object> export = new HashMap<>();
            
            export.put("mapId", mapId);
            export.put("channelName", channel.getDisplayName(mapId));
            
            ChannelSettings settings = channel.getSettings(mapId);
            if (settings != null) {
                Map<String, Object> settingsData = new HashMap<>();
                settingsData.put("customName", settings.getCustomName());
                settingsData.put("tags", settings.getTags());
                settingsData.put("notificationsEnabled", settings.isNotificationsEnabled());
                settingsData.put("notificationLevel", settings.getNotificationLevel().name());
                export.put("settings", settingsData);
            }
            
            List<TelegraphMessage> messages = channel.getMessages(mapId);
            List<Map<String, Object>> messageData = new ArrayList<>();
            for (TelegraphMessage msg : messages) {
                Map<String, Object> data = new HashMap<>();
                data.put("content", msg.content());
                data.put("timestamp", msg.timestamp().toString());
                if (msg.type() != null) {
                    data.put("type", msg.type().name());
                }
                messageData.add(data);
            }
            export.put("messages", messageData);
            
            String json = gson.toJson(export);
            Files.writeString(exportPath, json);
            
        } catch (IOException e) {
            System.err.println("Failed to export channel data: " + e.getMessage());
        }
    }
}
