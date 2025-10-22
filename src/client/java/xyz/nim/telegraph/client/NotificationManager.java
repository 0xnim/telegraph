package xyz.nim.telegraph.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.Text;
import net.minecraft.sound.SoundEvents;
import xyz.nim.telegraph.client.protocol.CarniteProtocol;

public class NotificationManager {
    private final TelegraphChannel channel;
    
    public NotificationManager(TelegraphChannel channel) {
        this.channel = channel;
    }
    
    public void notifyBannerChange(BannerChangeEvent event) {
        ChannelSettings settings = channel.getSettings(event.mapId());
        
        if (!shouldNotify(settings, event.changeType())) {
            return;
        }
        
        showBannerToast(event, settings);
        playNotificationSound(event, settings);
    }
    
    public void notifyDecorationChange(MapDecorationChangeEvent event) {
        ChannelSettings settings = channel.getSettings(event.mapId());
        
        if (!shouldNotify(settings, 
            event.changeType() == MapDecorationChangeEvent.ChangeType.ADDED ? BannerChangeEvent.ChangeType.ADDED :
            event.changeType() == MapDecorationChangeEvent.ChangeType.REMOVED ? BannerChangeEvent.ChangeType.REMOVED :
            BannerChangeEvent.ChangeType.CHANGED)) {
            return;
        }
        
        showDecorationToast(event, settings);
        playNotificationSound(null, settings);
    }
    
    private boolean shouldNotify(ChannelSettings settings, BannerChangeEvent.ChangeType changeType) {
        if (settings == null || !settings.isNotificationsEnabled()) {
            return false;
        }
        
        if (settings.getNotificationLevel() == ChannelSettings.NotificationLevel.NONE) {
            return false;
        }
        
        if (settings.getNotificationLevel() == ChannelSettings.NotificationLevel.IMPORTANT_ONLY) {
            return changeType == BannerChangeEvent.ChangeType.ADDED;
        }
        
        return true;
    }
    
    private void showBannerToast(BannerChangeEvent event, ChannelSettings settings) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        
        String channelName = channel.getDisplayName(event.mapId());
        DecorationSnapshot decoration = event.decoration() != null ? event.decoration() : event.oldDecoration();
        String bannerText = decoration != null && decoration.name() != null ? decoration.name() : "Banner";
        String bannerColor = decoration != null ? getBannerColorName(decoration.type()) : "";
        
        String title = getNotificationTitle(event.changeType(), channelName, settings);
        String description = formatNotificationMessage(bannerText, bannerColor, event.changeType(), settings);
        
        SystemToast.Type toastType = getToastTypeForChange(event.changeType(), decoration, settings);
        
        client.getToastManager().add(
            SystemToast.create(
                client,
                toastType,
                Text.literal(title),
                Text.literal(description)
            )
        );
    }
    
    private void showDecorationToast(MapDecorationChangeEvent event, ChannelSettings settings) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        
        String channelName = channel.getDisplayName(event.mapId());
        DecorationSnapshot decoration = event.decoration() != null ? event.decoration() : event.oldDecoration();
        String decorationName = decoration != null && decoration.name() != null ? decoration.name() : "Decoration";
        
        BannerChangeEvent.ChangeType changeType = 
            event.changeType() == MapDecorationChangeEvent.ChangeType.ADDED ? BannerChangeEvent.ChangeType.ADDED :
            event.changeType() == MapDecorationChangeEvent.ChangeType.REMOVED ? BannerChangeEvent.ChangeType.REMOVED :
            BannerChangeEvent.ChangeType.CHANGED;
        
        String title = getNotificationTitle(changeType, channelName, settings);
        String description = decorationName;
        
        client.getToastManager().add(
            SystemToast.create(
                client,
                SystemToast.Type.PERIODIC_NOTIFICATION,
                Text.literal(title),
                Text.literal(description)
            )
        );
    }
    
    private String getNotificationTitle(BannerChangeEvent.ChangeType changeType, String channelName, ChannelSettings settings) {
        String prefix = switch (changeType) {
            case ADDED -> "📬 New message";
            case REMOVED -> "🗑 Message removed";
            case CHANGED -> "✏ Message changed";
        };
        
        return prefix + ": " + channelName;
    }
    
    private String formatNotificationMessage(String bannerText, String bannerColor, 
                                            BannerChangeEvent.ChangeType changeType, 
                                            ChannelSettings settings) {
        if (settings != null && settings.getProtocol() instanceof CarniteProtocol && !bannerColor.isEmpty()) {
            String tense = getCarniteColorTense(bannerColor);
            if (!tense.isEmpty()) {
                return "[" + tense + "] " + truncateText(bannerText, 40);
            }
        }
        
        return truncateText(bannerText, 45);
    }
    
    private String getCarniteColorTense(String bannerType) {
        if (bannerType == null) return "";
        
        if (bannerType.contains("white")) return "PRESENT";
        if (bannerType.contains("light_gray")) return "PAST";
        if (bannerType.contains("gray") && !bannerType.contains("light")) return "FUTURE";
        if (bannerType.contains("pink")) return "MIGHT";
        if (bannerType.contains("red")) return "⚠ URGENT";
        if (bannerType.contains("light_blue")) return "REQUEST";
        if (bannerType.contains("black")) return "DECIDED";
        if (bannerType.contains("blue") && !bannerType.contains("light")) return "QUESTION";
        if (bannerType.contains("yellow")) return "TRADE";
        if (bannerType.contains("purple") || bannerType.contains("magenta")) return "GOAL";
        
        return "";
    }
    
    private String getBannerColorName(String bannerType) {
        if (bannerType == null) return "";
        
        if (bannerType.contains("white")) return "White";
        if (bannerType.contains("orange")) return "Orange";
        if (bannerType.contains("magenta")) return "Magenta";
        if (bannerType.contains("light_blue")) return "Light Blue";
        if (bannerType.contains("yellow")) return "Yellow";
        if (bannerType.contains("lime")) return "Lime";
        if (bannerType.contains("pink")) return "Pink";
        if (bannerType.contains("gray") && !bannerType.contains("light")) return "Gray";
        if (bannerType.contains("light_gray")) return "Light Gray";
        if (bannerType.contains("cyan")) return "Cyan";
        if (bannerType.contains("purple")) return "Purple";
        if (bannerType.contains("blue") && !bannerType.contains("light")) return "Blue";
        if (bannerType.contains("brown")) return "Brown";
        if (bannerType.contains("green")) return "Green";
        if (bannerType.contains("red")) return "Red";
        if (bannerType.contains("black")) return "Black";
        
        return "";
    }
    
    private SystemToast.Type getToastTypeForChange(BannerChangeEvent.ChangeType changeType, 
                                                    DecorationSnapshot decoration, 
                                                    ChannelSettings settings) {
        if (decoration != null && decoration.type() != null && decoration.type().contains("red")) {
            return SystemToast.Type.NARRATOR_TOGGLE;
        }
        
        return SystemToast.Type.PERIODIC_NOTIFICATION;
    }
    
    private String truncateText(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
    
    private void playNotificationSound(BannerChangeEvent event, ChannelSettings settings) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) return;
        
        boolean isUrgent = event != null && event.decoration() != null && 
                          event.decoration().type() != null && 
                          event.decoration().type().contains("red");
        
        if (isUrgent) {
            client.player.playSound(SoundEvents.BLOCK_BELL_USE, 0.7f, 1.2f);
        } else {
            client.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.0f);
        }
    }
}
