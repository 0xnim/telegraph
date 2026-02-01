package xyz.nim.telegraph.client;

import xyz.nim.telegraph.client.protocol.CarniteProtocol;

import java.time.Instant;
import java.util.*;

public class TelegraphChannel {
    private final Map<Integer, String> channelNames = new HashMap<>();
    private final Map<Integer, List<TelegraphMessage>> messageHistory = new HashMap<>();
    private final Map<Integer, String> userSetNames = new HashMap<>();
    private final Map<Integer, ChannelSettings> channelSettings = new HashMap<>();
    private final Map<Integer, Instant> lastSeenTimestamp = new HashMap<>();
    private final Map<Integer, Set<String>> activeDecorations = new HashMap<>();
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
    
    public void addMessage(TelegraphMessage message) {
        List<TelegraphMessage> history = messageHistory.computeIfAbsent(message.mapId(), k -> new ArrayList<>());

        if (message.decoration() != null && message.type() != null) {
            String decoKey = getDecorationKey(message.decoration());
            Set<String> active = activeDecorations.computeIfAbsent(message.mapId(), k -> new HashSet<>());

            switch (message.type()) {
                case ADDED -> {
                    if (active.contains(decoKey)) {
                        return;
                    }
                    active.add(decoKey);
                }
                case REMOVED -> {
                    active.remove(decoKey);
                }
                case CHANGED -> {
                }
            }
        }

        history.add(message);
        if (history.size() > MAX_HISTORY) {
            history.remove(0);
        }
    }

    private String getDecorationKey(DecorationSnapshot deco) {
        return deco.type() + "_" + deco.x() + "_" + deco.z() + "_" + deco.name();
    }

    public boolean isDecorationActive(int mapId, DecorationSnapshot decoration) {
        if (decoration == null) {
            return false;
        }
        Set<String> active = activeDecorations.get(mapId);
        if (active == null) {
            return false;
        }
        return active.contains(getDecorationKey(decoration));
    }

    public boolean isMessageDecorationActive(TelegraphMessage message) {
        return isDecorationActive(message.mapId(), message.decoration());
    }

    public List<TelegraphMessage> getMessages(int mapId) {
        List<TelegraphMessage> msgs = messageHistory.getOrDefault(mapId, Collections.emptyList());
        return new ArrayList<>(msgs);
    }

    public boolean removeMessage(int mapId, TelegraphMessage message) {
        List<TelegraphMessage> history = messageHistory.get(mapId);
        if (history == null) return false;

        boolean removed = history.remove(message);

        if (removed && message.decoration() != null && message.type() == TelegraphMessage.ChangeType.ADDED) {
            Set<String> active = activeDecorations.get(mapId);
            if (active != null) {
                active.remove(getDecorationKey(message.decoration()));
            }
        }

        return removed;
    }

    public void clearMessages(int mapId) {
        List<TelegraphMessage> history = messageHistory.get(mapId);
        if (history != null) {
            history.clear();
        }
        Set<String> active = activeDecorations.get(mapId);
        if (active != null) {
            active.clear();
        }
    }
    
    public void removeChannel(int mapId) {
        channelNames.remove(mapId);
        userSetNames.remove(mapId);
        messageHistory.remove(mapId);
        activeDecorations.remove(mapId);
    }
    
    public boolean ensureChannelExists(int mapId) {
        if (!messageHistory.containsKey(mapId)) {
            messageHistory.put(mapId, new ArrayList<>());
            return true;
        }
        return false;
    }
    
    public void addWelcomeMessage(int mapId) {
        TelegraphMessage welcomeMsg = new TelegraphMessage(
            mapId,
            "Channel tracking started",
            null,
            java.time.Instant.now(),
            null
        );
        messageHistory.computeIfAbsent(mapId, k -> new ArrayList<>()).add(welcomeMsg);
    }
    
    public void addTestMessage(int mapId) {
        List<TelegraphMessage> messages = messageHistory.computeIfAbsent(mapId, k -> new ArrayList<>());
        
        TelegraphMessage.ChangeType[] types = TelegraphMessage.ChangeType.values();
        TelegraphMessage.ChangeType randomType = types[new java.util.Random().nextInt(types.length)];
        
        TelegraphMessage testMsg = new TelegraphMessage(
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

    public void markAsRead(int mapId) {
        lastSeenTimestamp.put(mapId, Instant.now());
    }

    public int getUnreadCount(int mapId) {
        Instant lastSeen = lastSeenTimestamp.get(mapId);
        if (lastSeen == null) {
            return getMessages(mapId).size();
        }
        return (int) getMessages(mapId).stream()
            .filter(msg -> msg.timestamp().isAfter(lastSeen))
            .count();
    }

    public boolean hasUnread(int mapId) {
        return getUnreadCount(mapId) > 0;
    }

    public Instant getLastSeenTimestamp(int mapId) {
        return lastSeenTimestamp.get(mapId);
    }

    public void setLastSeenTimestamp(int mapId, Instant timestamp) {
        if (timestamp != null) {
            lastSeenTimestamp.put(mapId, timestamp);
        }
    }

    public Map<Integer, Instant> getAllLastSeenTimestamps() {
        return new HashMap<>(lastSeenTimestamp);
    }

    public ChannelMetadata getMetadata(int mapId) {
        List<TelegraphMessage> messages = getMessages(mapId);
        ChannelSettings settings = getSettings(mapId);

        Instant lastActivity = messages.stream()
            .map(TelegraphMessage::timestamp)
            .max(Instant::compareTo)
            .orElse(null);

        String protocolName = "Telegraph";
        if (settings != null && settings.getProtocol() instanceof CarniteProtocol) {
            protocolName = "Carnite";
        }

        return new ChannelMetadata(
            mapId,
            getDisplayName(mapId),
            messages.size(),
            lastActivity,
            protocolName,
            isArchived(mapId),
            settings != null ? settings.getNotificationLevel() : ChannelSettings.NotificationLevel.ALL,
            getTags(mapId),
            getUnreadCount(mapId)
        );
    }

    public record ChannelMetadata(
        int mapId,
        String displayName,
        int messageCount,
        Instant lastActivity,
        String protocolName,
        boolean archived,
        ChannelSettings.NotificationLevel notificationLevel,
        List<String> tags,
        int unreadCount
    ) {}

    public void clear() {
        channelNames.clear();
        messageHistory.clear();
        userSetNames.clear();
        channelSettings.clear();
        lastSeenTimestamp.clear();
        activeDecorations.clear();
    }
}
