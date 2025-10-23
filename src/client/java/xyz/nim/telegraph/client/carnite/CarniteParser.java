package xyz.nim.telegraph.client.carnite;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CarniteParser {
    
    public static ParsedCarniteMessage parse(String message, String bannerColor) {
        if (message == null || message.isEmpty()) {
            return new ParsedCarniteMessage(message, bannerColor, Collections.emptyList(), null);
        }
        
        CarniteMessageType type = determineMessageType(message);
        List<CarniteToken> tokens = tokenize(message);
        CarniteGrammar grammar = analyzeGrammar(tokens, type);
        
        return new ParsedCarniteMessage(message, bannerColor, tokens, grammar);
    }
    
    private static CarniteMessageType determineMessageType(String message) {
        if (message.contains("_")) return CarniteMessageType.QUESTION;
        if (message.startsWith("^")) return CarniteMessageType.RESPONSE;
        if (message.contains(":") && message.contains(";")) return CarniteMessageType.TRADE;
        return CarniteMessageType.STATEMENT;
    }
    
    private static List<CarniteToken> tokenize(String message) {
        List<CarniteToken> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        
        for (int i = 0; i < message.length(); i++) {
            char c = message.charAt(i);
            
            if (isSymbol(c)) {
                if (current.length() > 0) {
                    tokens.add(new CarniteToken(current.toString(), CarniteTokenType.WORD));
                    current = new StringBuilder();
                }
                
                if (c == '\'' && i + 1 < message.length()) {
                    int end = message.indexOf('\'', i + 1);
                    if (end != -1) {
                        tokens.add(new CarniteToken(message.substring(i, end + 1), CarniteTokenType.QUOTED));
                        i = end;
                        continue;
                    }
                }
                
                tokens.add(new CarniteToken(String.valueOf(c), getSymbolType(c)));
            } else if (c == ' ') {
                if (current.length() > 0) {
                    tokens.add(new CarniteToken(current.toString(), CarniteTokenType.WORD));
                    current = new StringBuilder();
                }
            } else {
                current.append(c);
            }
        }
        
        if (current.length() > 0) {
            tokens.add(new CarniteToken(current.toString(), CarniteTokenType.WORD));
        }
        
        return tokens;
    }
    
    private static boolean isSymbol(char c) {
        return c == '|' || c == ':' || c == ';' || c == ',' || c == '&' || 
               c == '.' || c == '_' || c == '^' || c == '\'' || c == '~' || c == '-';
    }
    
    private static CarniteTokenType getSymbolType(char c) {
        return switch (c) {
            case '|' -> CarniteTokenType.AGENT;
            case ':' -> CarniteTokenType.YOUR_CIV;
            case ';' -> CarniteTokenType.MY_CIV;
            case ',' -> CarniteTokenType.PROPERTY;
            case '&' -> CarniteTokenType.AND;
            case '.' -> CarniteTokenType.STACK;
            case '_' -> CarniteTokenType.QUESTION_BLANK;
            case '^' -> CarniteTokenType.RESPONSE;
            case '~' -> CarniteTokenType.PLURAL;
            case '-' -> CarniteTokenType.NEGATION;
            default -> CarniteTokenType.SYMBOL;
        };
    }
    
    private static CarniteGrammar analyzeGrammar(List<CarniteToken> tokens, CarniteMessageType type) {
        List<String> components = new ArrayList<>();
        
        for (CarniteToken token : tokens) {
            if (token.type == CarniteTokenType.WORD) {
                components.add(token.value);
            }
        }
        
        return new CarniteGrammar(type, components);
    }
    
    public static String getTenseFromColor(String bannerColor) {
        if (bannerColor == null) return "Unknown";
        
        if (bannerColor.contains("white")) return "Present Statement";
        if (bannerColor.contains("light_gray")) return "Past Statement";
        if (bannerColor.contains("gray") && !bannerColor.contains("light")) return "Future Statement";
        if (bannerColor.contains("pink")) return "Conditional/Might";
        if (bannerColor.contains("red")) return "Urgent/High Priority";
        if (bannerColor.contains("light_blue")) return "Request/Command";
        if (bannerColor.contains("black")) return "Opinion/Decision";
        if (bannerColor.contains("blue") && !bannerColor.contains("light")) return "Y/N Question";
        if (bannerColor.contains("yellow")) return "Trade Offer";
        if (bannerColor.contains("purple") || bannerColor.contains("magenta")) return "Goal/Objective";
        
        return "Unknown";
    }
    
    public static List<String> extractCivAbbreviations(String message) {
        List<String> civs = new ArrayList<>();
        Matcher matcher = CarniteConstants.CIV_ABBR_PATTERN.matcher(message);
        
        while (matcher.find()) {
            civs.add(matcher.group());
        }
        
        return civs;
    }
    
    public static boolean isTradeMessage(String message) {
        return message.contains(";") && message.contains(":");
    }
    
    public static TradeOffer parseTradeOffer(String message) {
        if (!isTradeMessage(message)) {
            return null;
        }
        
        String[] parts = message.split(";");
        if (parts.length < 2) return null;
        
        String offering = parts[0].trim();
        String requesting = parts.length > 1 ? parts[1].split(":")[0].trim() : "";
        
        return new TradeOffer(offering, requesting);
    }
    
    public enum CarniteMessageType {
        STATEMENT,
        QUESTION,
        RESPONSE,
        TRADE
    }
    
    public enum CarniteTokenType {
        WORD,
        AGENT,
        YOUR_CIV,
        MY_CIV,
        PROPERTY,
        AND,
        STACK,
        QUESTION_BLANK,
        RESPONSE,
        PLURAL,
        NEGATION,
        QUOTED,
        SYMBOL
    }
    
    public record CarniteToken(String value, CarniteTokenType type) {}
    
    public record ParsedCarniteMessage(
        String original,
        String bannerColor,
        List<CarniteToken> tokens,
        CarniteGrammar grammar
    ) {}
    
    public record CarniteGrammar(
        CarniteMessageType type,
        List<String> components
    ) {}
    
    public record TradeOffer(String offering, String requesting) {}
}
