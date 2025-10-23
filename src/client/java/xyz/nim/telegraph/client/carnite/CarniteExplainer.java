package xyz.nim.telegraph.client.carnite;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CarniteExplainer {
    
    public static ExplanationResult explainMessage(String message, String bannerColor) {
        List<MessagePart> parts = new ArrayList<>();
        
        if (message == null || message.isEmpty()) {
            return new ExplanationResult(Collections.emptyList(), "Empty message", "");
        }
        
        // Parse the message into parts
        List<CarniteParser.CarniteToken> tokens = CarniteParser.parse(message, bannerColor).tokens();
        
        int position = 0;
        for (CarniteParser.CarniteToken token : tokens) {
            String explanation = explainToken(token);
            String expanded = expandToken(token);
            MessagePartType type = getPartType(token);
            
            parts.add(new MessagePart(token.value(), explanation, expanded, type, position, position + token.value().length()));
            position += token.value().length() + 1; // +1 for space
        }
        
        // Get overall structure
        String structure = analyzeStructure(message, tokens);
        String translation = translateToEnglish(message, bannerColor, tokens);
        
        return new ExplanationResult(parts, structure, translation);
    }
    
    private static String explainToken(CarniteParser.CarniteToken token) {
        return switch (token.type()) {
            case AGENT -> "Individual/Player marker - indicates this is a specific person";
            case YOUR_CIV -> "Your civilization - addressing another civ";
            case MY_CIV -> "My civilization - refers to speaker's civ (we/us)";
            case PROPERTY -> "Property/of - links descriptors to nouns";
            case AND -> "And - joins two related items together";
            case STACK -> "Stack of " + CarniteConstants.STACK_SIZE + " items - use before number for stacks";
            case QUESTION_BLANK -> "Question blank - what you're asking about";
            case RESPONSE -> "Response marker - this is replying to previous message";
            case PLURAL -> "Plural/about - indicates multiple or approximate";
            case NEGATION -> "Negation - means 'not' or 'do not'";
            case QUOTED -> "Quoted term - referring to specific word/phrase";
            case WORD -> {
                String word = token.value();
                String expanded = CarniteVocabulary.expand(word);
                if (!expanded.equals(word)) {
                    yield "Abbreviation for: " + expanded;
                }
                
                if (CarniteConstants.isNumber(word)) {
                    yield "Number: " + word;
                }
                
                if (CarniteConstants.isCivAbbreviation(word)) {
                    yield "Civilization abbreviation";
                }
                
                yield "Word/term";
            }
            case SYMBOL -> "Symbol";
        };
    }
    
    private static String expandToken(CarniteParser.CarniteToken token) {
        if (token.type() == CarniteParser.CarniteTokenType.WORD) {
            String word = token.value();
            String expanded = CarniteVocabulary.expand(word);
            if (!expanded.equals(word)) {
                return expanded;
            }
            
            Matcher matcher = CarniteConstants.NUM_ABBR_PATTERN.matcher(word);
            if (matcher.matches()) {
                String num = matcher.group(1);
                String abbr = matcher.group(2);
                String exp = CarniteVocabulary.expand(abbr);
                return num + " " + exp + (Integer.parseInt(num) > 1 ? "s" : "");
            }
        }
        return token.value();
    }
    
    private static MessagePartType getPartType(CarniteParser.CarniteToken token) {
        return switch (token.type()) {
            case AGENT, YOUR_CIV, MY_CIV -> MessagePartType.ENTITY;
            case PROPERTY, AND -> MessagePartType.CONNECTOR;
            case STACK -> MessagePartType.QUANTITY;
            case QUESTION_BLANK -> MessagePartType.QUESTION;
            case RESPONSE -> MessagePartType.RESPONSE;
            case PLURAL, NEGATION -> MessagePartType.MODIFIER;
            case QUOTED -> MessagePartType.QUOTE;
            case WORD -> {
                String word = token.value();
                if (CarniteConstants.isNumber(word)) yield MessagePartType.QUANTITY;
                if (CarniteConstants.isCivAbbreviation(word)) yield MessagePartType.ENTITY;
                String expanded = CarniteVocabulary.expand(word);
                if (!expanded.equals(word)) yield MessagePartType.ABBREVIATION;
                yield MessagePartType.WORD;
            }
            case SYMBOL -> MessagePartType.SYMBOL;
        };
    }
    
    private static String analyzeStructure(String message, List<CarniteParser.CarniteToken> tokens) {
        StringBuilder structure = new StringBuilder("Message Structure:\n");
        
        boolean hasSubject = false;
        boolean hasVerb = false;
        
        for (CarniteParser.CarniteToken token : tokens) {
            if (token.type() == CarniteParser.CarniteTokenType.WORD) {
                String expanded = CarniteVocabulary.expand(token.value());
                if (CarniteConstants.isVerb(expanded)) {
                    hasVerb = true;
                }
            }
            
            if (token.type() == CarniteParser.CarniteTokenType.YOUR_CIV || 
                token.type() == CarniteParser.CarniteTokenType.MY_CIV) {
                hasSubject = true;
            }
        }
        
        if (message.contains("_")) {
            structure.append("• Type: QUESTION (contains question blank _)\n");
        } else if (message.startsWith("^")) {
            structure.append("• Type: RESPONSE (starts with ^)\n");
        } else if (message.contains(";") && message.contains(":")) {
            structure.append("• Type: TRADE OFFER (contains ; and :)\n");
        } else {
            structure.append("• Type: STATEMENT\n");
        }
        
        structure.append("• Word Order: Od Oi S V (Object, Location/To, Who, Action)\n");
        
        if (hasSubject) structure.append("• Contains: Subject (who)\n");
        if (hasVerb) structure.append("• Contains: Verb (action)\n");
        
        return structure.toString();
    }
    
    private static String translateToEnglish(String message, String bannerColor, List<CarniteParser.CarniteToken> tokens) {
        CarniteTranslator.TranslationResult result = CarniteTranslator.translate(message, bannerColor);
        return result.translation();
    }
    
    public enum MessagePartType {
        ENTITY,        // Civs, players, agents
        CONNECTOR,     // ,, &
        QUANTITY,      // Numbers, stacks
        QUESTION,      // _
        RESPONSE,      // ^
        MODIFIER,      // ~, -
        QUOTE,         // ''
        ABBREVIATION,  // Abbreviated words
        WORD,          // Regular words
        SYMBOL         // Other symbols
    }
    
    public record MessagePart(
        String text,
        String explanation,
        String expanded,
        MessagePartType type,
        int startPos,
        int endPos
    ) {}
    
    public record ExplanationResult(
        List<MessagePart> parts,
        String structure,
        String translation
    ) {}
}
