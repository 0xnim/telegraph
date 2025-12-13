package xyz.nim.telegraph.client;

import xyz.nim.telegraph.client.protocol.CommunicationProtocol;
import xyz.nim.telegraph.client.protocol.MapTelegraphProtocol;
import xyz.nim.telegraph.client.protocol.transport.NoneTransport;
import xyz.nim.telegraph.client.protocol.transport.TransportProtocol;

import java.util.ArrayList;
import java.util.List;

public class ChannelSettings {
    private final int mapId;
    private String customName;
    private boolean notificationsEnabled;
    private boolean archived;
    private boolean showTranslations;
    private List<String> tags;
    private NotificationLevel notificationLevel;
    private CommunicationProtocol protocol;
    private String channelType;
    private TransportProtocol transportProtocol;
    
    public enum NotificationLevel {
        ALL,
        IMPORTANT_ONLY,
        NONE
    }
    
    public ChannelSettings(int mapId) {
        this.mapId = mapId;
        this.customName = null;
        this.notificationsEnabled = true;
        this.archived = false;
        this.showTranslations = true;
        this.tags = new ArrayList<>();
        this.notificationLevel = NotificationLevel.ALL;
        this.protocol = new MapTelegraphProtocol();
        this.channelType = MapTelegraphProtocol.CIVILIAN_CHANNEL;
        this.transportProtocol = new NoneTransport();
    }
    
    public int getMapId() {
        return mapId;
    }
    
    public String getCustomName() {
        return customName;
    }
    
    public void setCustomName(String customName) {
        this.customName = customName;
    }
    
    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }
    
    public void setNotificationsEnabled(boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }
    
    public boolean isArchived() {
        return archived;
    }
    
    public void setArchived(boolean archived) {
        this.archived = archived;
    }
    
    public List<String> getTags() {
        return new ArrayList<>(tags);
    }
    
    public void addTag(String tag) {
        if (!tags.contains(tag)) {
            tags.add(tag);
        }
    }
    
    public void removeTag(String tag) {
        tags.remove(tag);
    }
    
    public boolean hasTag(String tag) {
        return tags.contains(tag);
    }
    
    public NotificationLevel getNotificationLevel() {
        return notificationLevel;
    }
    
    public void setNotificationLevel(NotificationLevel notificationLevel) {
        this.notificationLevel = notificationLevel;
    }
    
    public CommunicationProtocol getProtocol() {
        return protocol;
    }
    
    public void setProtocol(CommunicationProtocol protocol) {
        this.protocol = protocol;
        if (!protocol.getChannelTypes().contains(this.channelType)) {
            this.channelType = protocol.getChannelTypes().isEmpty() ? null : protocol.getChannelTypes().get(0);
        }
    }
    
    public String getChannelType() {
        return channelType;
    }
    
    public void setChannelType(String channelType) {
        this.channelType = channelType;
    }
    
    public boolean isShowTranslations() {
        return showTranslations;
    }
    
    public void setShowTranslations(boolean showTranslations) {
        this.showTranslations = showTranslations;
    }

    public TransportProtocol getTransportProtocol() {
        return transportProtocol;
    }

    public void setTransportProtocol(TransportProtocol transportProtocol) {
        this.transportProtocol = transportProtocol;
    }
}
