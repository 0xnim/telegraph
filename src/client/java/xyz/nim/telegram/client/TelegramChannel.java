package xyz.nim.telegram.client;

import java.util.*;

public class TelegramChannel {
    private final Map<Integer, String> channelNames = new HashMap<>();
    private final Map<Integer, List<TelegramMessage>> messageHistory = new HashMap<>();
    private final Map<Integer, String> userSetNames = new HashMap<>();
    private final Map<Integer, ChannelSettings> channelSettings = new HashMap<>();
    private static final int MAX_HISTORY = 100;
    
    public void setChannelName(int mapId, String name) {
        if (name == null || name.isBlank()) {
            channelNames.remove(mapId);
        } else {
            channelNames.put(mapId, name);
        }
    }
    
    public void setUserChannelName(int mapId, String name) {
        if (name == null || name.isBlank()) {
            userSetNames.remove(mapId);
            getOrCreateSettings(mapId).setCustomName(null);
        } else {
            userSetNames.put(mapId, name);
            getOrCreateSettings(mapId).setCustomName(name);
        }
    }
    
    public Optional<String> getChannelName(int mapId) {
        return Optional.ofNullable(channelNames.get(mapId));
    }
    
    public String getDisplayName(int mapId) {
        if (userSetNames.containsKey(mapId)) {
            return userSetNames.get(mapId);
        }
        return channelNames.getOrDefault(mapId, "map_" + mapId);
    }
    
    public void addMessage(TelegramMessage message) {
        messageHistory.computeIfAbsent(message.mapId(), k -> new ArrayList<>()).add(message);
        List<TelegramMessage> history = messageHistory.get(message.mapId());
        if (history.size() > MAX_HISTORY) {
            history.remove(0);
        }
    }
    
    public List<TelegramMessage> getMessages(int mapId) {
        List<TelegramMessage> msgs = messageHistory.getOrDefault(mapId, Collections.emptyList());
        return new ArrayList<>(msgs);
    }
    
    public void removeChannel(int mapId) {
        channelNames.remove(mapId);
        userSetNames.remove(mapId);
        messageHistory.remove(mapId);
    }
    
    public boolean ensureChannelExists(int mapId) {
        if (!messageHistory.containsKey(mapId)) {
            messageHistory.put(mapId, new ArrayList<>());
            return true;
        }
        return false;
    }
    
    public void addWelcomeMessage(int mapId) {
        TelegramMessage welcomeMsg = new TelegramMessage(
            mapId,
            "Channel tracking started",
            null,
            java.time.Instant.now(),
            null
        );
        messageHistory.computeIfAbsent(mapId, k -> new ArrayList<>()).add(welcomeMsg);
    }
    
    public void addTestMessage(int mapId) {
        List<TelegramMessage> messages = messageHistory.computeIfAbsent(mapId, k -> new ArrayList<>());
        
        TelegramMessage.ChangeType[] types = TelegramMessage.ChangeType.values();
        TelegramMessage.ChangeType randomType = types[new java.util.Random().nextInt(types.length)];
        
        TelegramMessage testMsg = new TelegramMessage(
            mapId,
            "Test banner " + (messages.size() + 1),
            randomType,
            java.time.Instant.now(),
            null
        );
        messages.add(testMsg);
    }
    
    public Set<Integer> getAllChannelIds() {
        Set<Integer> ids = new HashSet<>();
        ids.addAll(channelNames.keySet());
        ids.addAll(messageHistory.keySet());
        ids.addAll(userSetNames.keySet());
        return ids;
    }
    
    public Map<Integer, String> getAllChannels() {
        Map<Integer, String> channels = new HashMap<>();
        for (int mapId : getAllChannelIds()) {
            if (!isArchived(mapId)) {
                channels.put(mapId, getDisplayName(mapId));
            }
        }
        return channels;
    }
    
    public Map<Integer, String> getAllChannelsIncludingArchived() {
        Map<Integer, String> channels = new HashMap<>();
        for (int mapId : getAllChannelIds()) {
            channels.put(mapId, getDisplayName(mapId));
        }
        return channels;
    }
    
    public ChannelSettings getSettings(int mapId) {
        return channelSettings.get(mapId);
    }
    
    public ChannelSettings getOrCreateSettings(int mapId) {
        return channelSettings.computeIfAbsent(mapId, ChannelSettings::new);
    }
    
    public boolean isArchived(int mapId) {
        ChannelSettings settings = channelSettings.get(mapId);
        return settings != null && settings.isArchived();
    }
    
    public void setArchived(int mapId, boolean archived) {
        getOrCreateSettings(mapId).setArchived(archived);
    }
    
    public void addTag(int mapId, String tag) {
        getOrCreateSettings(mapId).addTag(tag);
    }
    
    public void removeTag(int mapId, String tag) {
        ChannelSettings settings = channelSettings.get(mapId);
        if (settings != null) {
            settings.removeTag(tag);
        }
    }
    
    public List<String> getTags(int mapId) {
        ChannelSettings settings = channelSettings.get(mapId);
        return settings != null ? settings.getTags() : new ArrayList<>();
    }
    
    public Set<String> getAllTags() {
        Set<String> allTags = new HashSet<>();
        for (ChannelSettings settings : channelSettings.values()) {
            allTags.addAll(settings.getTags());
        }
        return allTags;
    }
    
    public Map<Integer, ChannelSettings> getAllSettings() {
        return new HashMap<>(channelSettings);
    }
}
