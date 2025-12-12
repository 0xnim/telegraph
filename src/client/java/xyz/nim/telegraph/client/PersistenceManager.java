package xyz.nim.telegraph.client;

import com.google.gson.*;
import net.minecraft.client.MinecraftClient;
import xyz.nim.telegraph.client.carnite.CarniteVocabulary;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

public class PersistenceManager {
    private static final String CONFIG_DIR = "config/telegraph";
    private static final String SETTINGS_FILE = "channel_settings.json";
    private static final String MESSAGES_FILE = "messages.json";
    private static final String CIVILIZATIONS_FILE = "civilizations.json";
    private static final String GLOBAL_SETTINGS_FILE = "global_settings.json";
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
                settingData.put("showTranslations", s.isShowTranslations());
                settingData.put("tags", s.getTags());
                settingData.put("notificationLevel", s.getNotificationLevel().name());
                
                serialized.put(entry.getKey(), settingData);
            }
            
            Map<Integer, String> userNames = new HashMap<>();
            for (int mapId : channel.getAllChannelIds()) {
                channel.getChannelName(mapId).ifPresent(name -> userNames.put(mapId, name));
            }
            
            Map<String, Long> lastSeen = new HashMap<>();
            for (Map.Entry<Integer, Instant> entry : channel.getAllLastSeenTimestamps().entrySet()) {
                lastSeen.put(String.valueOf(entry.getKey()), entry.getValue().toEpochMilli());
            }

            Map<String, Object> data = new HashMap<>();
            data.put("settings", serialized);
            data.put("userNames", userNames);
            data.put("lastSeen", lastSeen);

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
                    if (settingData.has("showTranslations")) {
                        settings.setShowTranslations(settingData.get("showTranslations").getAsBoolean());
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

            if (root.has("lastSeen")) {
                JsonObject lastSeenObj = root.getAsJsonObject("lastSeen");
                for (Map.Entry<String, JsonElement> entry : lastSeenObj.entrySet()) {
                    int mapId = Integer.parseInt(entry.getKey());
                    long epochMilli = entry.getValue().getAsLong();
                    channel.setLastSeenTimestamp(mapId, Instant.ofEpochMilli(epochMilli));
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
    
    public void saveCivilizations() {
        Path configDir = getConfigDir();
        if (configDir == null) return;
        
        Path civsPath = configDir.resolve(CIVILIZATIONS_FILE);
        
        try {
            Map<String, String> civilizations = CarniteVocabulary.getAllCivilizations();
            String json = gson.toJson(civilizations);
            Files.writeString(civsPath, json);
        } catch (IOException e) {
            System.err.println("Failed to save civilizations: " + e.getMessage());
        }
    }
    
    public void loadMessages(TelegraphChannel channel) {
        Path configDir = getConfigDir();
        if (configDir == null) return;
        
        Path messagesPath = configDir.resolve(MESSAGES_FILE);
        if (!Files.exists(messagesPath)) return;
        
        try {
            String json = Files.readString(messagesPath);
            JsonObject root = gson.fromJson(json, JsonObject.class);
            
            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                int mapId = Integer.parseInt(entry.getKey());
                JsonArray messagesArray = entry.getValue().getAsJsonArray();
                
                for (JsonElement msgElement : messagesArray) {
                    JsonObject msgObj = msgElement.getAsJsonObject();
                    
                    String content = msgObj.get("content").getAsString();
                    Instant timestamp = Instant.ofEpochMilli(msgObj.get("timestamp").getAsLong());
                    
                    TelegraphMessage.ChangeType type = null;
                    if (msgObj.has("type") && !msgObj.get("type").isJsonNull()) {
                        type = TelegraphMessage.ChangeType.valueOf(msgObj.get("type").getAsString());
                    }
                    
                    DecorationSnapshot decoration = null;
                    if (msgObj.has("decoration") && !msgObj.get("decoration").isJsonNull()) {
                        JsonObject decoObj = msgObj.getAsJsonObject("decoration");
                        decoration = new DecorationSnapshot(
                            decoObj.get("type").getAsString(),
                            decoObj.get("x").getAsDouble(),
                            decoObj.get("z").getAsDouble(),
                            decoObj.get("rotation").getAsInt(),
                            decoObj.has("name") && !decoObj.get("name").isJsonNull() ? 
                                decoObj.get("name").getAsString() : null
                        );
                    }
                    
                    TelegraphMessage message = new TelegraphMessage(mapId, content, type, timestamp, decoration);
                    channel.addMessage(message);
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to load messages: " + e.getMessage());
        }
    }
    
    public void saveGlobalSettings(boolean mapRefreshEnabled) {
        Path configDir = getConfigDir();
        if (configDir == null) return;
        
        Path settingsPath = configDir.resolve(GLOBAL_SETTINGS_FILE);
        
        try {
            Map<String, Object> settings = new HashMap<>();
            settings.put("mapRefreshEnabled", mapRefreshEnabled);
            
            String json = gson.toJson(settings);
            Files.writeString(settingsPath, json);
        } catch (IOException e) {
            System.err.println("Failed to save global settings: " + e.getMessage());
        }
    }
    
    public boolean loadMapRefreshEnabled() {
        Path configDir = getConfigDir();
        if (configDir == null) return false;
        
        Path settingsPath = configDir.resolve(GLOBAL_SETTINGS_FILE);
        if (!Files.exists(settingsPath)) return false;
        
        try {
            String json = Files.readString(settingsPath);
            JsonObject settings = gson.fromJson(json, JsonObject.class);
            
            if (settings.has("mapRefreshEnabled")) {
                return settings.get("mapRefreshEnabled").getAsBoolean();
            }
        } catch (IOException e) {
            System.err.println("Failed to load global settings: " + e.getMessage());
        }
        return false;
    }
    
    public void loadCivilizations() {
        Path configDir = getConfigDir();
        if (configDir == null) return;
        
        Path civsPath = configDir.resolve(CIVILIZATIONS_FILE);
        if (!Files.exists(civsPath)) return;
        
        try {
            String json = Files.readString(civsPath);
            JsonObject civsObj = gson.fromJson(json, JsonObject.class);
            
            Map<String, String> civilizations = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : civsObj.entrySet()) {
                civilizations.put(entry.getKey(), entry.getValue().getAsString());
            }
            
            CarniteVocabulary.loadCivilizations(civilizations);
        } catch (IOException e) {
            System.err.println("Failed to load civilizations: " + e.getMessage());
        }
    }
}
