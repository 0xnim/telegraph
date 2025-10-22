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
            case STACK -> "Stack of 64 items - use before number for stacks";
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
                
                // Check if it's a number
                if (word.matches("\\d+")) {
                    yield "Number: " + word;
                }
                
                // Check if it's a civ abbreviation (all caps)
                if (word.matches("[A-Z]{2,4}")) {
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
            
            // Check for number + abbreviation
            Pattern pattern = Pattern.compile("(\\d+)([a-z]+)");
            Matcher matcher = pattern.matcher(word);
            if (matcher.matches()) {
                String num = matcher.group(1);
                String abbr = matcher.group(2);
                String exp = CarniteVocabulary.expand(abbr);
                return num + " " + exp + (Integer.parseInt(num) > 1 ? "s" : "");
            }
            
            // Check for stack notation
            if (word.contains(".")) {
                String[] parts = word.split("\\.");
                if (parts.length == 2) {
                    try {
                        int stacks = Integer.parseInt(parts[0]);
                        Pattern itemPattern = Pattern.compile("(\\d*)([a-z]+)");
                        Matcher itemMatcher = itemPattern.matcher(parts[1]);
                        if (itemMatcher.matches()) {
                            String remainder = itemMatcher.group(1);
                            String item = itemMatcher.group(2);
                            String itemExp = CarniteVocabulary.expand(item);
                            int total = stacks * 64 + (remainder.isEmpty() ? 0 : Integer.parseInt(remainder));
                            return total + " " + itemExp + "s (" + stacks + " stacks" + 
                                   (remainder.isEmpty() ? "" : " + " + remainder) + ")";
                        }
                    } catch (NumberFormatException ignored) {}
                }
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
                if (word.matches("\\d+")) yield MessagePartType.QUANTITY;
                if (word.matches("[A-Z]{2,4}")) yield MessagePartType.ENTITY;
                String expanded = CarniteVocabulary.expand(word);
                if (!expanded.equals(word)) yield MessagePartType.ABBREVIATION;
                yield MessagePartType.WORD;
            }
            case SYMBOL -> MessagePartType.SYMBOL;
        };
    }
    
    private static String analyzeStructure(String message, List<CarniteParser.CarniteToken> tokens) {
        StringBuilder structure = new StringBuilder("Message Structure:\n");
        
        // Detect word order
        boolean hasObject = false;
        boolean hasSubject = false;
        boolean hasVerb = false;
        
        for (CarniteParser.CarniteToken token : tokens) {
            if (token.type() == CarniteParser.CarniteTokenType.WORD) {
                String word = token.value();
                String expanded = CarniteVocabulary.expand(word);
                
                // Check if it's an action word (verb)
                if (isVerb(expanded)) {
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
    
    private static boolean isVerb(String word) {
        String[] verbs = {"attack", "raid", "steal", "take", "give", "trade", "receive", "move", 
                         "merge", "elect", "die", "kill", "surrender", "accept", "ally"};
        for (String verb : verbs) {
            if (word.equalsIgnoreCase(verb)) return true;
        }
        return false;
    }
    
    private static String translateToEnglish(String message, String bannerColor, List<CarniteParser.CarniteToken> tokens) {
        // Use the proper translation engine
        CarniteTranslator.TranslationResult result = CarniteTranslator.translate(message, bannerColor);
        return result.translation();
    }
    
    @Deprecated
    private static String translateToEnglishOld(String message, String bannerColor, List<CarniteParser.CarniteToken> tokens) {
        StringBuilder translation = new StringBuilder();
        
        // Get tense from banner color
        String tense = CarniteParser.getTenseFromColor(bannerColor);
        translation.append("[").append(tense).append("] ");
        
        // Check for special patterns
        boolean hasMyCiv = tokens.stream().anyMatch(t -> t.type() == CarniteParser.CarniteTokenType.MY_CIV);
        boolean hasYourCiv = tokens.stream().anyMatch(t -> t.type() == CarniteParser.CarniteTokenType.YOUR_CIV);
        boolean hasQuestion = tokens.stream().anyMatch(t -> t.type() == CarniteParser.CarniteTokenType.QUESTION_BLANK);
        boolean hasResponse = tokens.stream().anyMatch(t -> t.type() == CarniteParser.CarniteTokenType.RESPONSE);
        boolean isNegated = tokens.stream().anyMatch(t -> t.type() == CarniteParser.CarniteTokenType.NEGATION);
        
        // Trade pattern: items ; _:
        if (hasMyCiv && hasYourCiv && hasQuestion && message.contains(";") && message.contains(":")) {
            translation.append("My civilization offers ");
            
            List<String> items = new ArrayList<>();
            boolean beforeSemicolon = true;
            
            for (CarniteParser.CarniteToken token : tokens) {
                if (token.type() == CarniteParser.CarniteTokenType.MY_CIV) {
                    beforeSemicolon = false;
                    continue;
                }
                
                if (token.type() == CarniteParser.CarniteTokenType.YOUR_CIV || 
                    token.type() == CarniteParser.CarniteTokenType.QUESTION_BLANK) {
                    continue;
                }
                
                if (beforeSemicolon && token.type() == CarniteParser.CarniteTokenType.WORD) {
                    String expanded = expandToken(token);
                    if (!expanded.isEmpty() && !expanded.equals(token.value())) {
                        items.add(expanded);
                    }
                }
            }
            
            if (!items.isEmpty()) {
                translation.append(String.join(" and ", items));
            }
            
            translation.append(", what will you give?");
            return translation.toString();
        }
        
        // Question pattern
        if (hasQuestion && !message.contains(";") && !message.contains(":")) {
            List<String> civs = new ArrayList<>();
            List<String> verbs = new ArrayList<>();
            
            for (CarniteParser.CarniteToken token : tokens) {
                if (token.type() == CarniteParser.CarniteTokenType.WORD) {
                    String word = token.value();
                    if (word.matches("[A-Z]{2,4}")) {
                        civs.add(word);
                    } else if (isVerb(CarniteVocabulary.expand(word))) {
                        verbs.add(expandToken(token));
                    }
                }
            }
            
            translation.append("Who");
            if (!verbs.isEmpty()) {
                translation.append(" is ").append(verbs.get(0)).append("ing");
            }
            if (!civs.isEmpty()) {
                translation.append(" ").append(civs.get(0));
            }
            translation.append("?");
            return translation.toString();
        }
        
        // Response pattern
        if (hasResponse) {
            translation.append("In response: ");
            
            for (CarniteParser.CarniteToken token : tokens) {
                if (token.type() == CarniteParser.CarniteTokenType.WORD) {
                    String expanded = expandToken(token);
                    if (expanded.equals("y") || expanded.equals("yes")) {
                        translation.append("Yes");
                        return translation.toString();
                    } else if (expanded.equals("n") || expanded.equals("no")) {
                        translation.append("No");
                        return translation.toString();
                    }
                }
            }
        }
        
        // Standard pattern: Od Oi S V
        List<String> objects = new ArrayList<>();
        List<String> locations = new ArrayList<>();
        String subject = null;
        String verb = null;
        boolean negated = false;
        
        for (CarniteParser.CarniteToken token : tokens) {
            switch (token.type()) {
                case MY_CIV -> subject = "my civilization";
                case YOUR_CIV -> locations.add("to " + (tokens.indexOf(token) > 0 && 
                    tokens.get(tokens.indexOf(token) - 1).value().matches("[A-Z]{2,4}") ? 
                    tokens.get(tokens.indexOf(token) - 1).value() : "you"));
                case NEGATION -> negated = true;
                case WORD -> {
                    String word = token.value();
                    String expanded = expandToken(token);
                    
                    if (word.matches("[A-Z]{2,4}")) {
                        locations.add("at " + word);
                    } else if (isVerb(CarniteVocabulary.expand(word))) {
                        verb = expanded;
                    } else if (word.matches("\\d+.*") || expanded.contains("diamonds") || 
                               expanded.contains("iron") || expanded.contains("bread")) {
                        objects.add(expanded);
                    }
                }
                case PLURAL -> {
                    if (!objects.isEmpty()) {
                        int last = objects.size() - 1;
                        objects.set(last, "some " + objects.get(last));
                    }
                }
            }
        }
        
        // Construct sentence
        if (subject != null) {
            translation.append(subject);
        }
        
        if (verb != null) {
            if (negated) {
                translation.append(" does not");
            }
            translation.append(" ").append(verb);
        }
        
        if (!objects.isEmpty()) {
            translation.append(" ").append(String.join(", ", objects));
        }
        
        if (!locations.isEmpty()) {
            translation.append(" ").append(String.join(" ", locations));
        }
        
        return translation.toString().trim();
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
