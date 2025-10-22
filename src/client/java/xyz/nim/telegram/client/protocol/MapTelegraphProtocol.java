package xyz.nim.telegram.client.protocol;

import xyz.nim.telegram.client.TelegramMessage;

import java.util.Arrays;
import java.util.List;

public class MapTelegraphProtocol implements CommunicationProtocol {
    
    public static final String KOS_CHANNEL = "KOS/Wanted";
    public static final String MILITARY_CHANNEL = "Military";
    public static final String CIVILIAN_CHANNEL = "Civilian";
    
    @Override
    public String getName() {
        return "Map Telegraph Protocol";
    }
    
    @Override
    public String getDescription() {
        return "Standard inter-settlement communication protocol using three channels";
    }
    
    @Override
    public List<String> getChannelTypes() {
        return Arrays.asList(KOS_CHANNEL, MILITARY_CHANNEL, CIVILIAN_CHANNEL);
    }
    
    @Override
    public String formatMessage(TelegramMessage message, String channelType) {
        if (message.decoration() == null || message.decoration().name() == null) {
            return "";
        }
        
        String bannerText = message.decoration().name();
        
        return switch (channelType) {
            case KOS_CHANNEL -> formatKOSMessage(bannerText);
            case MILITARY_CHANNEL -> formatMilitaryMessage(bannerText);
            case CIVILIAN_CHANNEL -> formatCivilianMessage(bannerText);
            default -> bannerText;
        };
    }
    
    private String formatKOSMessage(String text) {
        return "[WANTED] " + text.toUpperCase();
    }
    
    private String formatMilitaryMessage(String text) {
        return "[MIL] " + text;
    }
    
    private String formatCivilianMessage(String text) {
        return text;
    }
    
    @Override
    public int getColorForChannelType(String channelType) {
        return switch (channelType) {
            case KOS_CHANNEL -> 0xFFFF5555;      // Red - danger
            case MILITARY_CHANNEL -> 0xFFFFAA00; // Orange - urgent
            case CIVILIAN_CHANNEL -> 0xFF55FF55; // Green - normal
            default -> 0xFFFFFFFF;
        };
    }
    
    @Override
    public String getChannelTypeDescription(String channelType) {
        return switch (channelType) {
            case KOS_CHANNEL -> "Raiders, terrorists, and KOS players. Least frequently updated.";
            case MILITARY_CHANNEL -> "Combat-related information. Requires rapid dissemination.";
            case CIVILIAN_CHANNEL -> "General news, diplomatic arrangements, and non-urgent communication.";
            default -> "Unknown channel type";
        };
    }
}
