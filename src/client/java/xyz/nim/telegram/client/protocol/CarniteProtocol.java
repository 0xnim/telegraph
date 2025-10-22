package xyz.nim.telegram.client.protocol;

import xyz.nim.telegram.client.TelegramMessage;

import java.util.Arrays;
import java.util.List;

public class CarniteProtocol implements CommunicationProtocol {
    
    public static final String PRESENT = "Present Statement";
    public static final String PAST = "Past Statement";
    public static final String FUTURE = "Future Statement";
    public static final String CONDITIONAL = "Conditional/Might";
    public static final String URGENT = "Urgent/High Priority";
    public static final String REQUEST = "Request/Command";
    public static final String DECISION = "Opinion/Decision";
    public static final String QUESTION = "Y/N Question";
    public static final String TRADE = "Trade Offer";
    public static final String GOAL = "Goal/Objective";
    
    @Override
    public String getName() {
        return "Carnite Telegraphic v1.0";
    }
    
    @Override
    public String getDescription() {
        return "Carnite Telegraphic v1.0 - A constructed language using banner colors for tense and symbols for grammar. Word order: Od Oi S V (What Where Who Action)";
    }
    
    @Override
    public List<String> getChannelTypes() {
        return Arrays.asList(
            PRESENT, PAST, FUTURE, CONDITIONAL, URGENT,
            REQUEST, DECISION, QUESTION, TRADE, GOAL
        );
    }
    
    @Override
    public String formatMessage(TelegramMessage message, String channelType) {
        if (message.decoration() == null || message.decoration().name() == null) {
            return "";
        }
        
        String text = message.decoration().name();
        String bannerColor = message.decoration().type();
        
        String tenseIndicator = getTenseFromBannerColor(bannerColor);
        if (tenseIndicator != null && !tenseIndicator.isEmpty()) {
            return "[" + tenseIndicator + "] " + text;
        }
        
        return text;
    }
    
    private String getTenseFromBannerColor(String bannerType) {
        if (bannerType == null) return "";
        
        if (bannerType.contains("white")) return "PRESENT";
        if (bannerType.contains("light_gray")) return "PAST";
        if (bannerType.contains("gray") && !bannerType.contains("light")) return "FUTURE";
        if (bannerType.contains("pink")) return "MIGHT";
        if (bannerType.contains("red")) return "URGENT!!!";
        if (bannerType.contains("light_blue")) return "REQUEST";
        if (bannerType.contains("black")) return "DECIDED";
        if (bannerType.contains("blue") && !bannerType.contains("light")) return "QUESTION?";
        if (bannerType.contains("yellow")) return "TRADE";
        if (bannerType.contains("purple") || bannerType.contains("magenta")) return "GOAL";
        
        return "";
    }
    
    @Override
    public int getColorForChannelType(String channelType) {
        return switch (channelType) {
            case PRESENT -> 0xFFFFFFFF;      // White
            case PAST -> 0xFFAAAAAA;         // Light Grey
            case FUTURE -> 0xFF555555;       // Dark Grey
            case CONDITIONAL -> 0xFFFF88FF;  // Pink
            case URGENT -> 0xFFFF0000;       // Red
            case REQUEST -> 0xFF88DDFF;      // Light Blue
            case DECISION -> 0xFF222222;     // Black
            case QUESTION -> 0xFF5555FF;     // Blue
            case TRADE -> 0xFFFFFF00;        // Yellow
            case GOAL -> 0xFFAA00FF;         // Purple
            default -> 0xFFFFFFFF;
        };
    }
    
    @Override
    public String getChannelTypeDescription(String channelType) {
        return switch (channelType) {
            case PRESENT -> "White banner. Present tense statements happening right now.";
            case PAST -> "Light Grey banner. Past tense statements about completed actions.";
            case FUTURE -> "Dark Grey banner. Future tense statements about planned actions.";
            case CONDITIONAL -> "Pink banner. Conditional statements expressing uncertainty or possibilities.";
            case URGENT -> "Red banner. Urgent/high priority present statements requiring immediate attention.";
            case REQUEST -> "Light Blue banner. Requests, commands, or suggestions for action.";
            case DECISION -> "Black banner. Opinions or decisions that have been made.";
            case QUESTION -> "Blue banner. Yes/No questions seeking confirmation or correction.";
            case TRADE -> "Yellow banner. Trade offers and negotiations (special grammar).";
            case GOAL -> "Purple banner. Current objectives or goals being pursued.";
            default -> "Unknown channel type";
        };
    }
}
