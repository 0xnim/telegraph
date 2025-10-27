package xyz.nim.telegraph.client.carnite;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A comprehensive Carnite → English translation engine that properly implements
 * the Od Oi S V (Object-direct, Object-indirect, Subject, Verb) grammar.
 * 
 * Carnite word order: Object-direct Object-indirect Subject Verb
 * Example: "~rd| ;" = "some raiders (Od) at my-civilization (Oi)" with implied "are at" verb
 */
public class CarniteTranslator {
    

    
    public static TranslationResult translate(String message, String bannerColor) {
        if (message == null || message.isEmpty()) {
            return new TranslationResult("", Collections.emptyList(), "");
        }
        
        CarniteParser.ParsedCarniteMessage parsed = CarniteParser.parse(message, bannerColor);
        TenseMode tenseMode = getTenseModeFromColor(bannerColor);
        
        // Detect message pattern - banner color affects pattern detection
        MessagePattern pattern = detectPattern(message, parsed.tokens(), tenseMode);
        
        // Translate based on pattern
        String translation = switch (pattern.type()) {
            case TRADE_OFFER -> translateTradeOffer(parsed.tokens(), tenseMode, message);
            case QUESTION -> translateQuestion(parsed.tokens(), tenseMode);
            case YES_NO_QUESTION -> translateYesNoQuestion(parsed.tokens(), tenseMode);
            case RESPONSE -> translateResponse(parsed.tokens(), tenseMode);
            case STATEMENT -> translateStatement(parsed.tokens(), tenseMode);
        };
        
        List<String> components = extractComponents(parsed.tokens());
        
        return new TranslationResult(translation, components, pattern.type().name());
    }
    
    private static TenseMode getTenseModeFromColor(String bannerColor) {
        if (bannerColor == null) return TenseMode.PRESENT;
        
        // Order matters: check more specific colors first
        if (bannerColor.contains("light_gray")) return TenseMode.PAST;
        if (bannerColor.contains("dark_gray")) return TenseMode.FUTURE;
        if (bannerColor.contains("light_blue")) return TenseMode.REQUEST;
        if (bannerColor.contains("white")) return TenseMode.PRESENT;
        if (bannerColor.contains("pink")) return TenseMode.CONDITIONAL;
        if (bannerColor.contains("red")) return TenseMode.URGENT;
        if (bannerColor.contains("black")) return TenseMode.DECISION;
        if (bannerColor.contains("blue")) return TenseMode.QUESTION;
        if (bannerColor.contains("yellow")) return TenseMode.TRADE;
        if (bannerColor.contains("purple") || bannerColor.contains("magenta")) return TenseMode.GOAL;
        if (bannerColor.contains("gray")) return TenseMode.FUTURE; // Fallback for "gray" without modifier
        
        return TenseMode.PRESENT;
    }
    
    private static MessagePattern detectPattern(String message, List<CarniteParser.CarniteToken> tokens, TenseMode tenseMode) {
        boolean hasSemicolon = tokens.stream().anyMatch(t -> t.type() == CarniteParser.CarniteTokenType.MY_CIV);
        boolean hasColon = tokens.stream().anyMatch(t -> t.type() == CarniteParser.CarniteTokenType.YOUR_CIV);
        boolean hasQuestion = tokens.stream().anyMatch(t -> t.type() == CarniteParser.CarniteTokenType.QUESTION_BLANK);
        boolean hasResponse = tokens.stream().anyMatch(t -> t.type() == CarniteParser.CarniteTokenType.RESPONSE);
        
        // CRITICAL: Blue banner (QUESTION tense) = Yes/No question, even without _ blank
        if (tenseMode == TenseMode.QUESTION && !hasQuestion) {
            return new MessagePattern(PatternType.YES_NO_QUESTION, 95);
        }
        
        // Yellow banner (TRADE tense) always means trade, even if pattern unclear
        if (tenseMode == TenseMode.TRADE) {
            return new MessagePattern(PatternType.TRADE_OFFER, 95);
        }
        
        if (hasResponse) {
            return new MessagePattern(PatternType.RESPONSE, 90);
        } else if (hasSemicolon && hasColon && hasQuestion) {
            return new MessagePattern(PatternType.TRADE_OFFER, 95);
        } else if (hasQuestion) {
            return new MessagePattern(PatternType.QUESTION, 85);
        } else {
            return new MessagePattern(PatternType.STATEMENT, 70);
        }
    }
    
    private static String translateTradeOffer(List<CarniteParser.CarniteToken> tokens, TenseMode tense, String originalMessage) {
        StringBuilder result = new StringBuilder();
        
        List<String> offerings = new ArrayList<>();
        boolean beforeMyCiv = true;
        boolean isResponse = false;
        String responseTo = null;
        
        for (int i = 0; i < tokens.size(); i++) {
            CarniteParser.CarniteToken token = tokens.get(i);
            
            if (token.type() == CarniteParser.CarniteTokenType.MY_CIV) {
                beforeMyCiv = false;
                continue;
            }
            
            if (token.type() == CarniteParser.CarniteTokenType.RESPONSE) {
                isResponse = true;
                // Check if next tokens are CIV:
                if (i + 1 < tokens.size() && tokens.get(i + 1).type() == CarniteParser.CarniteTokenType.WORD) {
                    String civCode = tokens.get(i + 1).value();
                    if (CarniteConstants.isCivAbbreviation(civCode)) {
                        responseTo = CarniteVocabulary.getCivilizationName(civCode);
                    }
                }
                continue;
            }
            
            if (token.type() == CarniteParser.CarniteTokenType.YOUR_CIV || 
                token.type() == CarniteParser.CarniteTokenType.QUESTION_BLANK) {
                continue;
            }
            
            if (beforeMyCiv) {
                if (token.type() == CarniteParser.CarniteTokenType.STACK && 
                    i + 1 < tokens.size() && tokens.get(i + 1).type() == CarniteParser.CarniteTokenType.WORD) {
                    // Check if previous token was a number for stack count
                    int stackCount = 1;
                    if (i > 0 && tokens.get(i - 1).type() == CarniteParser.CarniteTokenType.WORD) {
                        String prevWord = tokens.get(i - 1).value();
                        if (prevWord.matches("\\d+")) {
                            stackCount = Integer.parseInt(prevWord);
                            // Remove the number from offerings if it was added
                            if (!offerings.isEmpty() && offerings.get(offerings.size() - 1).equals(prevWord)) {
                                offerings.remove(offerings.size() - 1);
                            }
                        }
                    }
                    
                    String item = tokens.get(i + 1).value();
                    String itemName = CarniteVocabulary.expand(item.replaceAll("\\d+", ""));
                    if (!CarniteConstants.MASS_NOUNS.contains(itemName)) {
                        itemName = pluralize(itemName);
                    }
                    String stackWord = stackCount == 1 ? "stack" : "stacks";
                    offerings.add(stackCount + " " + stackWord + " of " + itemName);
                    i++; // Skip next token
                } else if (token.type() == CarniteParser.CarniteTokenType.WORD) {
                    String expanded = parseTradeNounPhrase(token.value());
                    if (!expanded.isEmpty()) {
                        offerings.add(expanded);
                    }
                }
            }
        }
        
        result.append("My civilization offers ");
        
        if (offerings.isEmpty()) {
            result.append("items");
        } else {
            result.append(formatList(offerings));
        }
        
        if (isResponse && responseTo != null) {
            result.append(" in response to ").append(responseTo).append("'s previous trade offer.");
        } else {
            result.append(". What will you give in return?");
        }
        
        return result.toString();
    }
    
    private static String translateQuestion(List<CarniteParser.CarniteToken> tokens, TenseMode tense) {
        StringBuilder result = new StringBuilder();
        
        // Parse components
        String questionWord = null;
        int questionPos = -1;
        boolean isAgentQuestion = false;
        boolean isLocationQuestion = false;
        String questionItem = null;
        
        List<String> objects = new ArrayList<>();
        List<String> locations = new ArrayList<>();
        List<String> agents = new ArrayList<>();
        String verb = null;
        boolean hasPlural = false;
        
        for (int i = 0; i < tokens.size(); i++) {
            CarniteParser.CarniteToken token = tokens.get(i);
            
            switch (token.type()) {
                case QUESTION_BLANK -> {
                    questionPos = i;
                    // Check if it's an agent question (_|)
                    if (i + 1 < tokens.size() && tokens.get(i + 1).type() == CarniteParser.CarniteTokenType.AGENT) {
                        isAgentQuestion = true;
                        i++; // Skip agent marker
                    }
                }
                
                case PLURAL -> hasPlural = true;
                
                case WORD -> {
                    String word = token.value();
                    String expanded = CarniteVocabulary.expand(word);
                    
                    if (word.equals("t") && questionPos >= 0) {
                        questionWord = "When";
                        continue;
                    }
                    if (word.equals("rsn") && questionPos >= 0) {
                        questionWord = "Why";
                        continue;
                    }
                    if (word.startsWith("_")) {
                        String item = word.substring(1);
                        questionItem = CarniteVocabulary.expand(item);
                        questionWord = "How many";
                        continue;
                    }
                    if (questionPos == i - 1 && !CarniteConstants.isCivAbbreviation(word) && !word.equals("t") && !word.equals("rsn")) {
                        // Check if it's a verb - if so, _ is "where" or other location question
                        if (!CarniteConstants.isVerb(expanded)) {
                            questionItem = CarniteVocabulary.expand(word);
                            questionWord = "How many";
                            continue;
                        }
                    }
                    
                    if (CarniteConstants.isCivAbbreviation(word)) {
                        String civName = CarniteVocabulary.getCivilizationName(word);
                        // In questions, CIV might be direct object or location
                        // Rules:
                        //   1. "CN _| atk" - CN is Od (no other objects yet)
                        //   2. "~dmd CV _| take" - CV is location (already have diamonds as Od)
                        //   3. "_| CV atk" - CV is location (after question)
                        
                        boolean hasQuestionAfter = false;
                        for (int j = i + 1; j < tokens.size(); j++) {
                            if (tokens.get(j).type() == CarniteParser.CarniteTokenType.QUESTION_BLANK) {
                                hasQuestionAfter = true;
                                break;
                            }
                        }
                        
                        if (hasQuestionAfter && objects.isEmpty()) {
                            // CIV before question and no objects yet: CIV is direct object
                            objects.add(civName);
                        } else {
                            // CIV after question, or already have objects: location
                            locations.add(civName);
                        }
                        continue;
                    }
                    
                    // Check if agent
                    if (i + 1 < tokens.size() && tokens.get(i + 1).type() == CarniteParser.CarniteTokenType.AGENT) {
                        // Check for level number after agent marker
                        String level = null;
                        if (i + 2 < tokens.size() && tokens.get(i + 2).type() == CarniteParser.CarniteTokenType.WORD) {
                            String potentialLevel = tokens.get(i + 2).value();
                            if (potentialLevel.matches("\\d+")) {
                                level = potentialLevel;
                            }
                        }
                        
                        String agentPhrase = parseAgentPhrase(word, hasPlural, level);
                        agents.add(agentPhrase);
                        hasPlural = false;
                        i++; // Skip agent marker
                        if (level != null) {
                            i++; // Skip level number too
                        }
                        continue;
                    }
                    
                    String verbForm = toVerbForm(expanded);
                    if (verbForm != null || CarniteConstants.isVerb(expanded)) {
                        verb = verbForm != null ? verbForm : expanded;
                        continue;
                    }
                    
                    // Otherwise it's an object
                    String nounPhrase = parseNounPhrase(word, hasPlural);
                    if (!nounPhrase.isEmpty()) {
                        objects.add(nounPhrase);
                        hasPlural = false;
                    }
                }
            }
        }
        
        // Determine question type if not already set
        if (questionWord == null) {
            if (isAgentQuestion) {
                questionWord = "Who";
            } else if (questionPos == 0) {
                // _ at start: asking about direct object (Od position in Od Oi S V)
                // If we have agents, it's asking "What did [agent] do?"
                // If no agents, it's asking "Who did [action]?"
                if (!agents.isEmpty()) {
                    // Have agents, so asking about object: "What did raiders steal?"
                    questionWord = "What";
                } else if (verb != null) {
                    // No agents, asking about who did the action
                    questionWord = "Who";
                } else {
                    questionWord = "What";
                }
            } else if (questionPos > 0 && questionPos == tokens.size() - 1) {
                questionWord = "How";
            } else {
                // Question in middle could be "Where" or "Which civ/What"
                if (locations.isEmpty() && !agents.isEmpty()) {
                    questionWord = "Where";
                    isLocationQuestion = true;
                } else {
                    questionWord = "Which civ/What";
                }
            }
        }
        
        // Build question sentence
        result.append(questionWord);
        
        if (questionItem != null) {
            // Pluralize for "How many"
            if (questionWord.equals("How many") && !CarniteConstants.MASS_NOUNS.contains(questionItem)) {
                result.append(" ").append(pluralize(questionItem));
            } else {
                result.append(" ").append(questionItem);
            }
        }
        
        // Handle different question word orders:
        // Content questions (_) use past tense by default per spec Table 3
        // All examples in Table 3 use past: "Who stole", "What did ... steal"
        boolean usePresentTense = false;
        
        if (isAgentQuestion || questionWord.equals("Which civ/What")) {
            // Who/Which: [Question] [verb-present/past] ...
            String agent = null;
            if (!agents.isEmpty()) {
                agent = agents.get(0);
                if (agent.toLowerCase().startsWith("some ")) {
                    agent = "the " + agent.substring(5);
                }
                result.append(" ").append(agent);
            }
            if (verb != null) {
                if (usePresentTense) {
                    // Use proper subject-verb agreement
                    boolean isPlural = agent != null && (agent.contains("the ") || agent.toLowerCase().contains("some"));
                    result.append(isPlural ? " are " : " is ").append(getIngForm(verb));
                } else {
                    String pastForm = getIrregularPast(verb);
                    result.append(" ").append(pastForm);
                }
            }
        } else if (questionWord.equals("What") && questionPos == 0) {
            // What did/is [agent] [verb/verbing]
            String agent = null;
            if (!agents.isEmpty()) {
                agent = agents.get(0);
                if (agent.toLowerCase().startsWith("some ")) {
                    agent = agent.substring(5);
                }
            }
            
            if (usePresentTense) {
                boolean isPlural = agent != null && agent.toLowerCase().contains("raiders");
                result.append(isPlural ? " are" : " is");
            } else {
                result.append(" did");
            }
            
            if (agent != null) {
                result.append(" ").append(agent);
            }
            if (verb != null) {
                if (usePresentTense) {
                    result.append(" ").append(getIngForm(verb));
                } else {
                    result.append(" ").append(verb);
                }
            }
        } else {
            // When/Where/Why/How/How many: [Question] did/is [agent] [verb/verbing]
            String agent = null;
            if (!agents.isEmpty()) {
                agent = agents.get(0);
                if (agent.toLowerCase().startsWith("some ")) {
                    if (questionWord.equals("Where")) {
                        agent = "the " + agent.substring(5);
                    } else {
                        agent = agent.substring(5);
                    }
                }
            }
            
            if (usePresentTense) {
                boolean isPlural = agent != null && (agent.contains("raiders") || agent.contains("the "));
                result.append(isPlural ? " are" : " is");
            } else {
                result.append(" did");
            }
            
            if (agent != null) {
                result.append(" ").append(agent);
            }
            if (verb != null) {
                if (usePresentTense) {
                    result.append(" ").append(getIngForm(verb));
                } else {
                    result.append(" ").append(verb);
                }
            }
        }
        
        // Add objects (strip "some" prefix for questions)
        if (!objects.isEmpty() && !questionWord.equals("What")) {
            List<String> cleanedObjects = new ArrayList<>();
            for (String obj : objects) {
                if (obj.toLowerCase().startsWith("some ")) {
                    cleanedObjects.add(obj.substring(5));
                } else if (obj.toLowerCase().startsWith("a ") || obj.toLowerCase().startsWith("an ")) {
                    cleanedObjects.add(obj.substring(obj.indexOf(' ') + 1));
                } else {
                    cleanedObjects.add(obj);
                }
            }
            result.append(" ").append(formatList(cleanedObjects));
        }
        
        // Add location with preposition
        if (!locations.isEmpty() && !isLocationQuestion) {
            String preposition = verb != null ? getPrepositionForVerb(verb) : "at";
            if (preposition.equals("from")) {
                result.append(" from ").append(locations.get(0));
            } else {
                result.append(" ").append(preposition).append(" ").append(locations.get(0));
            }
        } else if (isLocationQuestion) {
            result.append(" from");
        }
        
        result.append("?");
        
        return result.toString();
    }
    
    private static String translateYesNoQuestion(List<CarniteParser.CarniteToken> tokens, TenseMode tense) {
        // Blue banner question: "mtgm FTN" → "Is Fortun metagaming?"
        // "CN :: ; atk" → "Is my civilization attacking Carnation?"
        // "lib|5 CN:" → "Is there a level 5 librarian at Carnation?"
        // Parse using Od Oi S V structure, then convert to question form
        
        // First, parse the statement structure to get subject, verb, and objects
        String statementTranslation = translateStatement(tokens, tense);
        
        // If we got a proper statement, try to convert it to yes/no question
        // Pattern: "Subject verb object" → "Is subject verb+ing object?"
        
        List<String> directObjects = new ArrayList<>();
        List<String> indirectObjects = new ArrayList<>();
        String subject = null;
        String verb = null;
        boolean hasPlural = false;
        
        // Re-parse to get the components (same logic as translateStatement)
        for (int i = 0; i < tokens.size(); i++) {
            CarniteParser.CarniteToken token = tokens.get(i);
            
            switch (token.type()) {
                case PLURAL -> hasPlural = true;
                case MY_CIV -> {
                    // ; means my civilization is the subject (if followed by verb)
                    boolean hasVerbAfter = tokens.stream().skip(i + 1).anyMatch(t -> {
                        if (t.type() == CarniteParser.CarniteTokenType.WORD) {
                            String exp = CarniteVocabulary.expand(t.value());
                            return CarniteConstants.isVerb(exp);
                        }
                        return false;
                    });
                    if (hasVerbAfter) {
                        subject = "my civilization";
                    }
                }
                case YOUR_CIV -> {
                    // Check for previous token to determine if this is a location marker
                    boolean hasWordBefore = i > 0 && tokens.get(i - 1).type() == CarniteParser.CarniteTokenType.WORD;
                    if (hasWordBefore) {
                        String prevWord = tokens.get(i - 1).value();
                        if (CarniteConstants.isCivAbbreviation(prevWord)) {
                            String civName = CarniteVocabulary.getCivilizationName(prevWord);
                            indirectObjects.add(civName);
                            directObjects.remove(civName);
                        }
                    }
                    // Skip :: pattern
                    if (i + 1 < tokens.size() && tokens.get(i + 1).type() == CarniteParser.CarniteTokenType.YOUR_CIV) {
                        i++; // Skip second :
                    }
                }
                case AGENT -> {
                    // Handle agent phrases: lib|5 → "level 5 librarian"
                    if (i > 0 && tokens.get(i - 1).type() == CarniteParser.CarniteTokenType.WORD) {
                        String prevWord = tokens.get(i - 1).value();
                        String level = null;
                        
                        // Check for level number after agent marker
                        if (i + 1 < tokens.size() && tokens.get(i + 1).type() == CarniteParser.CarniteTokenType.WORD) {
                            String nextWord = tokens.get(i + 1).value();
                            if (nextWord.matches("\\d+")) {
                                level = nextWord;
                                i++; // Skip the level token
                            }
                        }
                        
                        String agentPhrase = parseAgentPhrase(prevWord, hasPlural, level);
                        // Remove the raw word that was added, replace with agent phrase
                        if (!directObjects.isEmpty() && directObjects.get(directObjects.size() - 1).contains(CarniteVocabulary.expand(prevWord))) {
                            directObjects.remove(directObjects.size() - 1);
                        }
                        directObjects.add(agentPhrase);
                        hasPlural = false;
                    }
                }
                case WORD -> {
                    String word = token.value();
                    String expanded = CarniteVocabulary.expand(word);
                    
                    // Skip if it's a level number (handled by AGENT case)
                    if (i > 0 && tokens.get(i - 1).type() == CarniteParser.CarniteTokenType.AGENT && word.matches("\\d+")) {
                        continue;
                    }
                    
                    // Check if it's a civ
                    if (CarniteConstants.isCivAbbreviation(word)) {
                        String civName = CarniteVocabulary.getCivilizationName(word);
                        // Check if followed by : (your civ marker)
                        boolean followedByYourCiv = i + 1 < tokens.size() && 
                            tokens.get(i + 1).type() == CarniteParser.CarniteTokenType.YOUR_CIV;
                        
                        if (followedByYourCiv) {
                            // Will be handled by YOUR_CIV case - add as temp object for now
                            directObjects.add(civName);
                        } else if (subject == null && verb == null) {
                            // Could be subject or object - check context
                            boolean followedByMyCiv = tokens.stream().skip(i + 1).anyMatch(t -> t.type() == CarniteParser.CarniteTokenType.MY_CIV);
                            if (followedByMyCiv) {
                                // "CN ; atk" pattern - CN is object
                                directObjects.add(civName);
                            } else {
                                // "FTN mtgm" pattern - FTN is subject
                                subject = civName;
                            }
                        } else {
                            directObjects.add(civName);
                        }
                        continue;
                    }
                    
                    // Check if verb
                    if (CarniteConstants.isVerb(expanded)) {
                        verb = expanded;
                        continue;
                    }
                    
                    // Skip if followed by agent marker (will be handled by AGENT case)
                    if (i + 1 < tokens.size() && tokens.get(i + 1).type() == CarniteParser.CarniteTokenType.AGENT) {
                        continue;
                    }
                    
                    // Otherwise treat as object
                    String nounPhrase = parseNounPhrase(word, hasPlural);
                    if (!nounPhrase.isEmpty()) {
                        directObjects.add(nounPhrase);
                        hasPlural = false;
                    }
                }
            }
        }
        
        // Build yes/no question: "Is [subject] [verb]ing [object]?"
        StringBuilder result = new StringBuilder();
        
        if (subject != null && verb != null) {
            result.append("Is ");
            result.append(subject);
            result.append(" ");
            result.append(verb);
            if (!verb.endsWith("ing")) {
                result.append("ing");
            }
            if (!directObjects.isEmpty()) {
                result.append(" ").append(formatList(directObjects));
            }
            if (!indirectObjects.isEmpty()) {
                result.append(" at ").append(formatList(indirectObjects));
            }
            result.append("?");
        } else if (verb != null) {
            // No explicit subject - use first location/object
            result.append("Is ");
            if (subject != null) {
                result.append(subject);
            } else if (!directObjects.isEmpty()) {
                result.append(directObjects.get(0));
            }
            result.append(" ").append(verb);
            if (!verb.endsWith("ing")) {
                result.append("ing");
            }
            result.append("?");
        } else if (!directObjects.isEmpty()) {
            result.append("Is there ");
            result.append(directObjects.get(0));
            if (!indirectObjects.isEmpty()) {
                result.append(" at ").append(formatList(indirectObjects));
            }
            result.append("?");
        } else {
            result.append("Is this true?");
        }
        
        return result.toString();
    }
    
    private static String translateResponse(List<CarniteParser.CarniteToken> tokens, TenseMode tense) {
        StringBuilder result = new StringBuilder();
        result.append("In response: ");
        
        boolean foundResponse = false;
        boolean hasNegation = false;
        List<String> content = new ArrayList<>();
        
        for (CarniteParser.CarniteToken token : tokens) {
            if (token.type() == CarniteParser.CarniteTokenType.RESPONSE) {
                foundResponse = true;
                continue;
            }
            
            if (token.type() == CarniteParser.CarniteTokenType.NEGATION) {
                hasNegation = true;
                continue;
            }
            
            if (foundResponse && token.type() == CarniteParser.CarniteTokenType.WORD) {
                String word = token.value();
                if (word.equals("y")) {
                    return result.append("Yes.").toString();
                } else if (word.equals("n")) {
                    return result.append("No.").toString();
                }
                String expanded = CarniteVocabulary.expand(word);
                content.add(expanded);
            }
        }
        
        if (hasNegation) {
            result.append("We do not ");
        } else {
            result.append("We ");
        }
        
        if (!content.isEmpty()) {
            result.append(String.join(" ", content));
        }
        
        result.append(".");
        
        return result.toString();
    }
    
    /**
     * Translate a statement following Od Oi S V grammar.
     * Object-direct comes first, then Object-indirect, then Subject, then Verb.
     */
    private static String translateStatement(List<CarniteParser.CarniteToken> tokens, TenseMode tense) {
        // Parse Od Oi S V structure
        List<String> directObjects = new ArrayList<>();
        List<String> indirectObjects = new ArrayList<>();
        String subject = null;
        String verb = null;
        boolean hasNegation = false;
        
        // Track modifiers for next word
        boolean hasPlural = false;
        boolean hasAgent = false;
        boolean directObjectIsAgent = false; // Track if first Od came from agent marker
        
        for (int i = 0; i < tokens.size(); i++) {
            CarniteParser.CarniteToken token = tokens.get(i);
            
            switch (token.type()) {
                case PLURAL -> hasPlural = true;
                case NEGATION -> hasNegation = true;
                case AGENT -> hasAgent = true;
                case PROPERTY -> {} // Skip - handled when building noun phrases
                case AND -> {} // Skip - just a connector between items
                
                case MY_CIV -> {
                    // Carnite word order: Od Oi S V
                    // MY_CIV (;) marks the subject position
                    // CIV; can mean:
                    //   1. CN ; atk → CN is Od, my civ is S
                    //   2. TWC; elct → TWC is S (the civ speaking)
                    //   3. CRS: CV; srd → CV is label for my civ
                    //   4. CN :: ; atk → CN is S, :: addresses everyone, ; is Od (my civ)
                    
                    String civBeforeSemicolon = null;
                    if (i > 0 && tokens.get(i - 1).type() == CarniteParser.CarniteTokenType.WORD) {
                        String prevWord = tokens.get(i - 1).value();
                        if (CarniteConstants.isCivAbbreviation(prevWord)) {
                            civBeforeSemicolon = CarniteVocabulary.getCivilizationName(prevWord);
                        }
                    }
                    
                    // Check what comes BEFORE the CIV: if there's a YOUR_CIV marker,
                    // this is "CRS: CV;" pattern = addressing CRS, my civ is CV
                    boolean hasYourCivBefore = false;
                    boolean hasDoubleYourCivBefore = false;
                    for (int j = 0; j < i; j++) {
                        if (tokens.get(j).type() == CarniteParser.CarniteTokenType.YOUR_CIV) {
                            hasYourCivBefore = true;
                            // Check if it's :: pattern
                            if (j + 1 < tokens.size() && 
                                tokens.get(j + 1).type() == CarniteParser.CarniteTokenType.YOUR_CIV) {
                                hasDoubleYourCivBefore = true;
                            }
                            break;
                        }
                    }
                    
                    // Check if there's a verb after
                    boolean hasVerbAfter = tokens.stream().skip(i + 1).anyMatch(t -> {
                        if (t.type() == CarniteParser.CarniteTokenType.WORD) {
                            String exp = CarniteVocabulary.expand(t.value());
                            return CarniteConstants.isVerb(exp) || toVerbForm(exp) != null;
                        }
                        return false;
                    });
                    
                    // Check if the CIV appeared earlier as a different role
                    final String finalCivName = civBeforeSemicolon;
                    boolean civAlreadyProcessed = (finalCivName != null) && 
                        (directObjects.stream().anyMatch(obj -> obj.contains(finalCivName)) ||
                         indirectObjects.stream().anyMatch(obj -> obj.contains(finalCivName)));
                    
                    // Pattern: "CN :: ; atk" - CN is already in directObjects, ; means my civ is S (subject)
                    // This is the same as "CN ; atk" - the :: doesn't change the grammar
                    // So this condition is now redundant and handled by the normal logic below
                    
                    if (civBeforeSemicolon != null) {
                        if (hasYourCivBefore && !hasDoubleYourCivBefore && hasVerbAfter) {
                            // Pattern: CRS: CV; srd
                            // CV is a label/context: "My civilization, Cannabis Village"
                            subject = "My civilization, " + civBeforeSemicolon;
                        } else if (civAlreadyProcessed && hasVerbAfter) {
                            // Pattern: CN ; atk (CN came first as Od and was added to directObjects)
                            // CIV was already added as direct object, ; means my civ is subject
                            subject = "My civilization";
                        } else if (hasVerbAfter) {
                            // Pattern: TWC; elct or 170| TWC; elct
                            // CIV is the subject itself (the civ speaking)
                            // Exception: if the civ is already in directObjects (from "CIV ; verb" pattern)
                            // then it's Od, not S
                            final String civName = civBeforeSemicolon; // Need final for lambda
                            if (directObjects.stream().anyMatch(obj -> obj.contains(civName))) {
                                // CIV is Od, ; means my civ is S
                                subject = "My civilization";
                            } else {
                                subject = civBeforeSemicolon;
                            }
                        } else {
                            // No verb after: location/context
                            indirectObjects.add(civBeforeSemicolon);
                        }
                    } else {
                        // Just ; with no CIV before
                        if (hasVerbAfter) {
                            subject = "My civilization";
                        } else {
                            indirectObjects.add("my civilization");
                        }
                    }
                }
                
                case YOUR_CIV -> {
                    // Check if next token is also YOUR_CIV (:: = "you all" / "to all civs on this line")
                    if (i + 1 < tokens.size() && tokens.get(i + 1).type() == CarniteParser.CarniteTokenType.YOUR_CIV) {
                        // Pattern: "CN :: ; atk" or "~:| :: gear"
                        // :: means addressing multiple people/civs - doesn't change grammar
                        // CN or :| should already be processed by the WORD handler
                        
                        // Check for pattern like "~:| :: gear" where we already have ":| " processed
                        // In this case, "You all" becomes the subject
                        if (directObjects.stream().anyMatch(obj -> obj.contains("citizen"))) {
                            subject = "You all";
                        }
                        // For "CN :: ; atk", CN is already in directObjects, just skip ::
                        i++; // Skip the second :
                        continue;
                    }
                    
                    // Check if next token is AGENT (:| = "your people/citizens")
                    if (i + 1 < tokens.size() && tokens.get(i + 1).type() == CarniteParser.CarniteTokenType.AGENT) {
                        String citizenPhrase = hasPlural ? "your citizens" : "your citizen";
                        directObjects.add(citizenPhrase);
                        hasPlural = false;
                        i++; // Skip the agent marker
                        continue;
                    }
                    
                    // YOUR_CIV marks destination: previous WORD was civ name
                    // Look back for the civ code
                    if (i > 0) {
                        for (int j = i - 1; j >= 0; j--) {
                            if (tokens.get(j).type() == CarniteParser.CarniteTokenType.WORD) {
                                String civCode = tokens.get(j).value();
                                if (CarniteConstants.isCivAbbreviation(civCode)) {
                                    String civName = CarniteVocabulary.getCivilizationName(civCode);
                                    indirectObjects.remove(civName);
                                    if (subject != null && subject.equals(civName)) {
                                        subject = null;
                                    }
                                    indirectObjects.add("to " + civName);
                                    break;
                                }
                            }
                        }
                    }
                }
                
                case STACK -> {
                    if (i + 1 < tokens.size() && tokens.get(i + 1).type() == CarniteParser.CarniteTokenType.WORD) {
                        String item = tokens.get(i + 1).value();
                        
                        // Check if previous token was a number (e.g., "2 . dmd" = 2 stacks)
                        int stackCount = 1;
                        int remainder = 0;
                        
                        if (i > 0 && tokens.get(i - 1).type() == CarniteParser.CarniteTokenType.WORD) {
                            String prevWord = tokens.get(i - 1).value();
                            if (prevWord.matches("\\d+")) {
                                stackCount = Integer.parseInt(prevWord);
                                // Remove the number from directObjects if it was added
                                if (!directObjects.isEmpty()) {
                                    directObjects.remove(directObjects.size() - 1);
                                }
                            }
                        }
                        
                        // Check if item has remainder (e.g., "32brd" in "3.32brd")
                        Pattern remainderPattern = Pattern.compile("(\\d+)([a-z]+)");
                        Matcher matcher = remainderPattern.matcher(item);
                        String itemName;
                        
                        if (matcher.matches()) {
                            remainder = Integer.parseInt(matcher.group(1));
                            itemName = CarniteVocabulary.expand(matcher.group(2));
                        } else {
                            itemName = CarniteVocabulary.expand(item);
                        }
                        
                        int total = stackCount * CarniteConstants.STACK_SIZE + remainder;
                        
                        if (!CarniteConstants.MASS_NOUNS.contains(itemName) && total > 1) {
                            itemName = pluralize(itemName);
                        }
                        
                        if (remainder > 0) {
                            directObjects.add(total + " " + itemName + " (" + stackCount + " stacks + " + remainder + ")");
                        } else if (stackCount == 1) {
                            directObjects.add(total + " " + itemName + " (1 stack)");
                        } else {
                            directObjects.add(total + " " + itemName + " (" + stackCount + " stacks)");
                        }
                        i++; // Skip next
                    }
                }
                
                case WORD -> {
                    String word = token.value();
                    String expanded = CarniteVocabulary.expand(word);
                    
                    if (CarniteConstants.isCivAbbreviation(word)) {
                        String civName = CarniteVocabulary.getCivilizationName(word);
                        
                        // SPECIAL CASE: If followed by property marker (,), this is a property phrase like "NM,smth|5"
                        // Don't treat as standalone civ - let it fall through to property/agent handling below
                        if (i + 1 < tokens.size() && tokens.get(i + 1).type() == CarniteParser.CarniteTokenType.PROPERTY) {
                            // Don't continue - fall through to property phrase building
                        } else {
                            // Normal civ handling
                        
                        // Look ahead for markers (not just immediate next token)
                        int k = i + 1;
                        boolean followedByMyCiv = false;
                        boolean followedByYourCiv = false;
                        boolean followedByDoubleYourCiv = false; // :: pattern
                        
                        while (k < tokens.size()) {
                            var t = tokens.get(k).type();
                            if (t == CarniteParser.CarniteTokenType.MY_CIV) {
                                followedByMyCiv = true;
                                break;
                            }
                            if (t == CarniteParser.CarniteTokenType.YOUR_CIV) {
                                followedByYourCiv = true;
                                // Check if followed by another YOUR_CIV (:: pattern)
                                if (k + 1 < tokens.size() && 
                                    tokens.get(k + 1).type() == CarniteParser.CarniteTokenType.YOUR_CIV) {
                                    followedByDoubleYourCiv = true;
                                }
                                break;
                            }
                            // Stop at hard boundaries
                            if (t == CarniteParser.CarniteTokenType.STACK || 
                                t == CarniteParser.CarniteTokenType.QUESTION_BLANK || 
                                t == CarniteParser.CarniteTokenType.RESPONSE) {
                                break;
                            }
                            // Skip soft markers
                            if (t != CarniteParser.CarniteTokenType.WORD && 
                                t != CarniteParser.CarniteTokenType.PROPERTY && 
                                t != CarniteParser.CarniteTokenType.PLURAL && 
                                t != CarniteParser.CarniteTokenType.AGENT) {
                                break;
                            }
                            k++;
                        }
                        
                        if (followedByDoubleYourCiv) {
                            // Pattern: "CN :: ; atk" - CN is Od (direct object)
                            // :: means "to all civs on this line", doesn't affect grammar
                            // Add CN to directObjects as Od
                            directObjects.add(civName);
                            continue;
                        } else if (followedByYourCiv) {
                            // CIV: pattern - this civ is an indirect object (destination)
                            // "2bld CN:" means "2 builders (Od) to Carnation (Oi)"
                            indirectObjects.add(civName);
                            continue;
                        } else if (followedByMyCiv) {
                            // Multiple patterns when CIV is followed by MY_CIV (;):
                            //   1. ".dmd CN ; trd" - .dmd is Od, CN is Oi, ; is S → "My civ traded diamonds to CN"
                            //   2. "CN ; atk" - CN is Od, ; is S → "My civ attacks CN"
                            //   3. "CRS: CV;" - CV is a label, YOUR_CIV marker present before
                            
                            boolean hasYourCivBefore = false;
                            for (int j = 0; j < i; j++) {
                                if (tokens.get(j).type() == CarniteParser.CarniteTokenType.YOUR_CIV) {
                                    hasYourCivBefore = true;
                                    break;
                                }
                            }
                            
                            if (hasYourCivBefore) {
                                // Pattern 3: skip, will be handled in MY_CIV case as label
                                continue;
                            }
                            
                            // Check if we already have a direct object before this civ
                            // If yes, then this civ might be Oi OR it might be S (subject)
                            // Pattern "170| TWC; elct" - 170| is Od, TWC is S (subject), not Oi
                            // Pattern ".dmd CN ; trd" - .dmd is Od, CN is Oi
                            // Key: if there's AGENT before the civ, the civ is likely S, not Oi
                            
                            boolean hasAgentBefore = false;
                            for (int j = i - 1; j >= 0; j--) {
                                if (tokens.get(j).type() == CarniteParser.CarniteTokenType.AGENT) {
                                    hasAgentBefore = true;
                                    break;
                                }
                                // Stop at ; or : boundaries
                                if (tokens.get(j).type() == CarniteParser.CarniteTokenType.MY_CIV ||
                                    tokens.get(j).type() == CarniteParser.CarniteTokenType.YOUR_CIV) {
                                    break;
                                }
                            }
                            
                            if (!directObjects.isEmpty() && !hasAgentBefore) {
                                // Pattern 1: We have Od already, no agent before civ, so civ is Oi
                                indirectObjects.add(civName);
                            } else if (directObjects.isEmpty()) {
                                // Pattern 2: No Od yet, so civ is Od
                                directObjects.add(civName);
                            } else if (hasAgentBefore) {
                                // Has agent before: CIV might be S (if right before ;) or location (if not)
                                // Check if this CIV is immediately before ;
                                boolean isImmediatelyBeforeSemicolon = (i + 1 < tokens.size() && 
                                    tokens.get(i + 1).type() == CarniteParser.CarniteTokenType.MY_CIV);
                                
                                if (!isImmediatelyBeforeSemicolon) {
                                    // Not right before ;, so it's a location
                                    // "~NM,rd| SF DR;" - SF is location, DR will be handled as S
                                    indirectObjects.add(civName);
                                }
                                // else: immediately before ;, will be handled as S in MY_CIV case
                            }
                            continue;
                        } else {
                            // Civ without marker: determine role based on context
                            // Check if previous token was & (AND) - if so, add to same list as previous
                            boolean hasAndBefore = (i > 0 && tokens.get(i - 1).type() == CarniteParser.CarniteTokenType.AND);
                            
                            if (hasAndBefore) {
                                // "CN&EG" - add EG to same list as CN
                                if (!directObjects.isEmpty()) {
                                    directObjects.add(civName);
                                } else {
                                    indirectObjects.add(civName);
                                }
                            } else if (subject == null) {
                                // First civ: could be Oi or S
                                if (indirectObjects.isEmpty()) {
                                    indirectObjects.add(civName);
                                } else {
                                    subject = civName;
                                }
                            } else {
                                // Already have subject: this is Od/Oi
                                indirectObjects.add(civName);
                            }
                        }
                        continue;
                        } // end else (normal civ handling)
                    }
                    
                    // FIRST: Build property phrase if there's a property marker
                    // This handles patterns like "NM,smth|5" where we need the full "NM,smth" before checking for |
                    // Pattern: "NM , smth | 5" should become fullWord = "NM,smth"
                    String fullWord = word;
                    if (i + 1 < tokens.size() && tokens.get(i + 1).type() == CarniteParser.CarniteTokenType.PROPERTY) {
                        StringBuilder propertyPhrase = new StringBuilder(word);
                        int savedI = i; // Save position in case we need to backtrack
                        while (i + 1 < tokens.size()) {
                            if (tokens.get(i + 1).type() == CarniteParser.CarniteTokenType.PROPERTY) {
                                i++;
                                propertyPhrase.append(",");
                            } else if (tokens.get(i + 1).type() == CarniteParser.CarniteTokenType.WORD) {
                                i++;
                                propertyPhrase.append(tokens.get(i).value());
                                // Check if next is another property marker (continue chain)
                                if (i + 1 < tokens.size() && 
                                    tokens.get(i + 1).type() == CarniteParser.CarniteTokenType.PROPERTY) {
                                    continue; // Keep building the chain
                                } else {
                                    break; // End of property chain
                                }
                            } else {
                                break; // Not part of property chain
                            }
                        }
                        fullWord = propertyPhrase.toString();
                    }
                    
                    // SECOND: Check if followed by agent marker
                    // This prevents "rd|" from being treated as verb "raid"
                    boolean willBeAgent = i + 1 < tokens.size() && 
                        tokens.get(i + 1).type() == CarniteParser.CarniteTokenType.AGENT;
                    
                    if (hasAgent || willBeAgent) {
                        // This is an agent noun - NOT a verb
                        // Check if there's a level number after the agent marker
                        String level = null;
                        if (willBeAgent && i + 2 < tokens.size() && 
                            tokens.get(i + 2).type() == CarniteParser.CarniteTokenType.WORD) {
                            String potentialLevel = tokens.get(i + 2).value();
                            if (potentialLevel.matches("\\d+")) {
                                level = potentialLevel;
                            }
                        }
                        
                        String agentPhrase = parseAgentPhrase(fullWord, hasPlural, level);
                        if (directObjects.isEmpty()) {
                            directObjectIsAgent = true; // First direct object is an agent
                        }
                        directObjects.add(agentPhrase);
                        hasPlural = false;
                        hasAgent = false;
                        if (willBeAgent) {
                            i++; // Skip agent marker
                            if (level != null) {
                                i++; // Skip level number too
                            }
                        }
                        continue;
                    }
                    
                    // THIRD: Check if this could be a verb (but not if hasPlural - that makes it a noun)
                    if (!hasPlural && verb == null) {
                        boolean shouldBeVerb = subject != null || !indirectObjects.isEmpty() || !directObjects.isEmpty() || hasNegation;
                        
                        // Direct verb match
                        if (shouldBeVerb && CarniteConstants.isVerb(expanded)) {
                            verb = expanded;
                            continue;
                        }
                        
                        // Check verbForm (words ending in "er" like "builder" -> "build")
                        String verbForm = toVerbForm(expanded);
                        if (shouldBeVerb && verbForm != null) {
                            verb = verbForm;
                            continue;
                        }
                    }
                    
                    // FINALLY: Treat as regular noun
                    String nounPhrase = parseNounPhrase(fullWord, hasPlural);
                    if (!nounPhrase.isEmpty()) {
                        directObjects.add(nounPhrase);
                        hasPlural = false;
                    }
                }
            }
        }
        
        // Build English sentence
        return constructSentence(directObjects, indirectObjects, subject, verb, hasNegation, tense, directObjectIsAgent);
    }
    
    /**
     * Construct an English sentence from parsed Od Oi S V components.
     */
    private static String constructSentence(List<String> directObjects, List<String> indirectObjects,
                                           String subject, String verb, boolean hasNegation, TenseMode tense,
                                           boolean directObjectIsAgent) {
        StringBuilder result = new StringBuilder();
        
        // No special prefix for URGENT tense anymore - just use normal conjugation
        
        // Simple case: just indirect object(s) with no Od/V (e.g., ";" alone or "CN:" or "CN&EG")
        if (directObjects.isEmpty() && verb == null && subject == null && !indirectObjects.isEmpty()) {
            if (indirectObjects.size() == 1) {
                String ioFirst = indirectObjects.get(0);
                if (ioFirst.equals("my civilization")) {
                    return "My civilization.";
                } else if (ioFirst.startsWith("to ")) {
                    return ioFirst.substring(3) + ".";
                }
                return capitalize(ioFirst) + ".";
            } else {
                return capitalize(formatList(indirectObjects)) + ".";
            }
        }
        
        // Case: Od Oi with no explicit subject/verb
        // Grammar determines meaning, not word content
        if (!directObjects.isEmpty() && !indirectObjects.isEmpty() && subject == null && verb == null) {
            String od = directObjects.get(0);
            String oi = indirectObjects.get(0);
            
            // Clean up location format
            String location = oi.equals("my civilization") ? oi : 
                            (oi.startsWith("to ") ? oi.substring(3) : oi);
            
            // Determine plurality for verb conjugation
            boolean isPlural = od.toLowerCase().startsWith("some") || 
                             od.toLowerCase().startsWith("a ") == false && 
                             od.toLowerCase().startsWith("an ") == false;
            
            // Rule 1: If Od is an agent (has | marker), use possession/presence structure
            // "lib|5 CM" → "CM has a level 5 librarian"
            // "~rd| ;" → "Some raiders are at my civilization"
            if (directObjectIsAgent) {
                // Special case: REQUEST tense with "my civilization" location
                if (tense == TenseMode.REQUEST && oi.equals("my civilization")) {
                    result.append("My civilization should have ");
                    result.append(od.toLowerCase());
                    result.append(".");
                    return result.toString();
                }
                
                // For non-REQUEST or non-self-location: use "be at" structure
                // Apply tense to implicit "be at" verb
                result.append(capitalize(od));
                result.append(" ");
                result.append(conjugateBeVerb(isPlural, tense));
                result.append(" at ").append(location);
                result.append(".");
                
                if (tense == TenseMode.CONDITIONAL) {
                    result.append(" Undecided.");
                }
                
                return result.toString();
            }
            
            // Rule 2: Non-agent Od with Oi destination
            // Default interpretation depends on tense
            if (tense == TenseMode.REQUEST) {
                // REQUEST: implies transfer/send
                // "2dmd CN:" on light_blue → "My civilization should send 2 diamonds to Carnation"
                if (oi.equals("my civilization")) {
                    result.append("My civilization should have ");
                    result.append(od.toLowerCase());
                } else {
                    result.append("My civilization should send ");
                    result.append(od);
                    result.append(" to ").append(location);
                }
                result.append(".");
                return result.toString();
            }
            
            // Rule 3: Default for other tenses: existence/location statement
            // "dmd CN" → "Diamonds are at Carnation" (tense from banner)
            result.append(capitalize(od));
            result.append(" ");
            result.append(conjugateBeVerb(isPlural, tense));
            result.append(" at ").append(location);
            result.append(".");
            
            if (tense == TenseMode.CONDITIONAL) {
                result.append(" Undecided.");
            }
            
            return result.toString();
        }
        
        // Case: Subject + Verb (e.g., "; mov" = "my civ might move")
        if (subject == null && verb != null && !indirectObjects.isEmpty() && indirectObjects.get(0).equals("my civilization")) {
            // Special case for GOAL tense: format as goal statement
            if (tense == TenseMode.GOAL) {
                result.append("My civilization's current goal is to ");
                result.append(verb);
            } else {
                result.append("My civilization");
                result.append(" ");
                result.append(conjugateVerb(verb, "My civilization", hasNegation, tense));
            }
            
            if (!directObjects.isEmpty()) {
                result.append(" ").append(formatList(directObjects));
            }
            
            result.append(".");
            
            if (tense == TenseMode.CONDITIONAL) {
                result.append(" Undecided.");
            }
            
            return result.toString();
        }
        
        // Case: Od Oi S V (full structure, e.g., ".dmd CN ; trd" = "My civilization traded diamonds to CN")
        if (!indirectObjects.isEmpty() && verb != null) {
            // Check if MY_CIV is in indirectObjects (makes it the subject)
            boolean myCivIsSubject = indirectObjects.stream().anyMatch(io -> io.equals("my civilization"));
            
            if (myCivIsSubject) {
                result.append("My civilization");
                result.append(" ");
                result.append(conjugateVerb(verb, "My civilization", hasNegation, tense));
                
                if (!directObjects.isEmpty()) {
                    result.append(" ").append(formatList(directObjects));
                }
                
                // Add other indirect objects (destinations)
                indirectObjects.stream()
                    .filter(io -> !io.equals("my civilization"))
                    .forEach(io -> result.append(" ").append(io));
                
                result.append(".");
                return result.toString();
            }
        }
        
        // Case: Subject + Verb + Objects
        if (subject != null && verb != null) {
            // Special case for DECISION tense with "my civilization" subject
            if (tense == TenseMode.DECISION && subject.startsWith("My civilization")) {
                result.append("It was decided that ");
                result.append(subject.toLowerCase());
                result.append(" will ");
                result.append(verb);
            } else if (tense == TenseMode.GOAL && subject.startsWith("My civilization")) {
                // Special case for GOAL tense: format as goal statement
                result.append("My civilization's current goal is to ");
                result.append(verb);
            } else {
                // Handle subject with comma (e.g., "My civilization, Cannabis Village")
                // Add comma before verb if not already present
                if (subject.contains(",") && !subject.endsWith(",")) {
                    // Find last word before verb and add comma
                    result.append(subject).append(",");
                } else {
                    result.append(subject);
                }
                result.append(" ");
                result.append(conjugateVerb(verb, subject, hasNegation, tense));
            }
            
            if (!directObjects.isEmpty()) {
                result.append(" ").append(formatList(directObjects));
            }
            
            // Add preposition based on verb
            if (!indirectObjects.isEmpty()) {
                // Add "right now" for present tense with build verb and "for" preposition
                boolean addRightNow = tense == TenseMode.PRESENT && verb.equals("build");
                
                if (addRightNow) {
                    result.append(" right now");
                }
                String preposition = getPrepositionForVerb(verb);
                for (String io : indirectObjects) {
                    if (!io.startsWith("to ") && !io.startsWith("at ") && !io.equals("my civilization")) {
                        result.append(" ").append(preposition).append(" ").append(io);
                    } else if (io.equals("my civilization")) {
                        // Skip - this was the subject marker
                    } else if (io.startsWith("to ")) {
                        result.append(" ").append(io);
                    } else {
                        result.append(" ").append(io);
                    }
                }
            }
            
            result.append(".");
            
            if (tense == TenseMode.CONDITIONAL) {
                result.append(" Undecided.");
            }
            
            return result.toString();
        }
        
        // Fallback: check if we have all Od Oi S V components scattered
        if (!directObjects.isEmpty() && subject != null && verb != null) {
            result.append(subject);
            result.append(" ");
            result.append(conjugateVerb(verb, subject, hasNegation, tense));
            result.append(" ").append(formatList(directObjects));
            
            if (tense == TenseMode.PRESENT && (verb.equals("build") || verb.equals("attack"))) {
                result.append(" right now");
            }
            
            if (!indirectObjects.isEmpty()) {
                String preposition = getPrepositionForVerb(verb);
                result.append(" ").append(preposition).append(" ").append(String.join(", ", indirectObjects));
            }
            
            result.append(".");
            return result.toString();
        }
        
        // Case: Od V (intransitive verb - direct object becomes subject)
        // e.g., "NM,smth|5 die" = "Nowy Madagaskar's level 5 blacksmith is dying"
        if (!directObjects.isEmpty() && verb != null && subject == null && indirectObjects.isEmpty()) {
            String objectAsSubject = capitalize(formatList(directObjects));
            result.append(objectAsSubject);
            result.append(" ");
            result.append(conjugateVerb(verb, objectAsSubject, hasNegation, tense));
            
            result.append(".");
            
            if (tense == TenseMode.CONDITIONAL) {
                result.append(" Undecided.");
            }
            
            return result.toString();
        }
        
        // Case: Just objects (no subject/verb)
        if (!directObjects.isEmpty()) {
            result.append(capitalize(formatList(directObjects))).append(".");
            return result.toString();
        }
        
        // Case: negation with verb but no subject
        if (hasNegation && verb != null && directObjects.isEmpty() && subject == null) {
            return "Not " + verb + ".";
        }
        
        return result.append("(unknown)").toString();
    }
    
    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
    
    /**
     * Get the appropriate preposition for a verb's indirect object.
     */
    private static String getPrepositionForVerb(String verb) {
        return switch (verb) {
            case "attack", "raid" -> "at";
            case "steal", "take", "receive" -> "from";
            case "give", "trade", "send" -> "to";
            case "build" -> "for";
            default -> "at";
        };
    }
    
    /**
     * Parse noun phrase for trade offers.
     */
    private static String parseTradeNounPhrase(String word) {
        Matcher matcher = CarniteConstants.NUM_ABBR_PATTERN.matcher(word);
        
        if (matcher.matches()) {
            int count = Integer.parseInt(matcher.group(1));
            String item = matcher.group(2);
            String itemName = expandWithProperties(item);
            if (count > 1 && !CarniteConstants.MASS_NOUNS.contains(itemName)) {
                itemName = pluralize(itemName);
            }
            return count + " " + itemName;
        }
        
        // Handle property notation: blss,fd
        if (word.contains(",")) {
            return expandWithProperties(word);
        }
        
        // Simple expansion
        String expanded = CarniteVocabulary.expand(word);
        if (!expanded.equals(word)) {
            return "an " + expanded;
        }
        
        return "";
    }
    
    /**
     * Pluralize a noun (simple English pluralization).
     */
    private static String pluralize(String noun) {
        if (noun.endsWith("s") || noun.endsWith("x") || noun.endsWith("z") ||
            noun.endsWith("ch") || noun.endsWith("sh")) {
            return noun + "es";
        }
        if (noun.endsWith("y") && noun.length() > 1 && !isVowel(noun.charAt(noun.length() - 2))) {
            return noun.substring(0, noun.length() - 1) + "ies";
        }
        return noun + "s";
    }
    
    private static boolean isVowel(char c) {
        return "aeiouAEIOU".indexOf(c) >= 0;
    }
    
    /**
     * Get past tense of verb (handles irregular verbs).
     */
    private static String getIrregularPast(String verb) {
        return switch (verb) {
            case "steal" -> "stole";
            case "take" -> "took";
            case "give" -> "gave";
            case "build" -> "built";
            case "send" -> "sent";
            default -> verb.endsWith("e") ? verb + "d" : verb + "ed";
        };
    }
    
    /**
     * Get -ing form of verb (handles 'e' dropping and 'ie' -> 'ying').
     */
    private static String getIngForm(String verb) {
        if (verb.endsWith("ie")) {
            // "die" -> "dying", "lie" -> "lying"
            return verb.substring(0, verb.length() - 2) + "ying";
        } else if (verb.endsWith("e") && !verb.endsWith("ee")) {
            // "take" -> "taking", "move" -> "moving", but "see" -> "seeing"
            return verb.substring(0, verb.length() - 1) + "ing";
        } else {
            return verb + "ing";
        }
    }
    
    /**
     * Parse a noun phrase with quantity, stacks, and properties.
     * Mass nouns are never pluralized.
     */
    private static String parseNounPhrase(String word, boolean hasPlural) {
        Matcher matcher = CarniteConstants.STACK_PATTERN.matcher(word);
        
        if (matcher.matches()) {
            int stacks = Integer.parseInt(matcher.group(1));
            String remainder = matcher.group(2);
            String item = matcher.group(3);
            String itemName = expandWithProperties(item);
            
            int remainderNum = remainder.isEmpty() ? 0 : Integer.parseInt(remainder);
            int total = stacks * CarniteConstants.STACK_SIZE + remainderNum;
            
            return total + " " + itemName + " (" + stacks + " stacks" + 
                   (remainderNum > 0 ? " + " + remainderNum : "") + ")";
        }
        
        matcher = CarniteConstants.NUM_ABBR_PATTERN.matcher(word);
        
        if (matcher.matches()) {
            int count = Integer.parseInt(matcher.group(1));
            String item = matcher.group(2);
            String itemName = expandWithProperties(item);
            
            // Check if itemName contains a mass noun (e.g., "blessed food")
            final String finalItemName = itemName;
            boolean isMassNoun = CarniteConstants.MASS_NOUNS.stream()
                .anyMatch(mass -> finalItemName.equals(mass) || finalItemName.endsWith(" " + mass));
            
            // Check if this is an agent/role noun (builder, miner, etc.) - convert to agent form
            String agentNoun = toAgentNoun(itemName);
            boolean isAgentRole = !agentNoun.equals(itemName + "er") || 
                                  itemName.equals("build") || itemName.equals("mine") || 
                                  itemName.equals("trade") || itemName.equals("raid");
            
            if (isAgentRole) {
                // This is a role/agent: use agent noun form
                itemName = agentNoun;
            }
            
            // Pluralize if count > 1 (unless it's a mass noun)
            if (count > 1 && !isMassNoun) {
                itemName = pluralize(itemName);
            }
            
            // If hasPlural is true, it means ~ was before the number (e.g., ~16blss,fd)
            if (hasPlural) {
                return "Around " + count + " " + itemName;
            }
            return count + " " + itemName;
        }
        
        // Handle property notation: blss,fd
        if (word.contains(",")) {
            String expanded = expandWithProperties(word);
            if (hasPlural) {
                return "Some " + expanded;
            }
            return "A piece of " + expanded;
        }
        
        // Simple expansion
        String expanded = CarniteVocabulary.expand(word);
        if (!expanded.equals(word)) {
            if (hasPlural) {
                // Check if it's a verb that should become agent noun (raid -> raiders, attack -> attackers)
                // Only do this for verbs, not regular nouns like "diamond"
                if (CarniteConstants.isVerb(expanded)) {
                    String agentNoun = toAgentNoun(expanded);
                    return "some " + pluralize(agentNoun);
                }
                // Regular noun - just pluralize
                return "some " + pluralize(expanded);
            }
            // Add "a" or "an" based on first letter
            char first = expanded.toLowerCase().charAt(0);
            if ("aeiou".indexOf(first) >= 0) {
                return "an " + expanded;
            }
            return "a " + expanded;
        }
        
        return "";
    }
    
    /**
     * Parse agent phrase (words with | marker).
     */
    private static String parseAgentPhrase(String word, boolean hasPlural, String level) {
        if (CarniteConstants.isNumber(word)) {
            String result = "player " + word;
            if (level != null) {
                result = "level " + level + " player " + word;
            }
            return result;
        }
        
        Matcher matcher = CarniteConstants.NUM_ABBR_PATTERN.matcher(word);
        
        if (matcher.matches()) {
            int count = Integer.parseInt(matcher.group(1));
            String role = matcher.group(2);
            String roleName = expandWithProperties(role);
            String agentNoun = toAgentNoun(roleName);
            String levelPhrase = level != null ? "level " + level + " " : "";
            String pluralForm = count > 1 ? agentNoun + "s" : agentNoun;
            return count + " " + levelPhrase + pluralForm;
        }
        
        // Handle "NM,smth" property notation - civ name + role
        String roleName = expandWithProperties(word);
        String agentNoun = toAgentNoun(roleName);
        
        // Check if this has a civ property (contains space after expansion)
        if (roleName.contains(" ")) {
            // "NM,smth|5" -> "Nowy Madagaskar's level 5 blacksmith" (singular/specific)
            // "~NM,rd|" -> "the Nowy Madagaskar raiders" (plural)
            // Find the last space to separate civ name from role
            int lastSpace = roleName.lastIndexOf(" ");
            String civName = roleName.substring(0, lastSpace);
            String role = roleName.substring(lastSpace + 1);
            String roleAgentNoun = toAgentNoun(role);
            
            if (hasPlural) {
                // Plural: use article form "the Nowy Madagaskar raiders"
                String levelPhrase = level != null ? "level " + level + " " : "";
                return "the " + civName + " " + levelPhrase + pluralize(roleAgentNoun);
            } else {
                // Singular: use possessive form
                String levelPhrase = level != null ? "level " + level + " " : "";
                return civName + "'s " + levelPhrase + roleAgentNoun;
            }
        }
        
        // Handle plural agents: ~rd| = "some raiders"
        if (hasPlural) {
            String levelPhrase = level != null ? "level " + level + " " : "";
            return "some " + levelPhrase + agentNoun + "s";
        }
        
        // Singular agent with level: "a level 5 librarian"
        if (level != null) {
            return "a level " + level + " " + agentNoun;
        }
        return "a " + agentNoun;
    }
    
    /**
     * Expand word with property handling (comma notation).
     */
    private static String expandWithProperties(String word) {
        if (word.contains(",")) {
            String[] parts = word.split(",");
            StringBuilder result = new StringBuilder();
            for (String part : parts) {
                if (result.length() > 0) result.append(" ");
                result.append(CarniteVocabulary.expand(part));
            }
            return result.toString();
        }
        return CarniteVocabulary.expand(word);
    }
    
    /**
     * Conjugate the "be" verb based on plurality and tense for location statements.
     */
    private static String conjugateBeVerb(boolean isPlural, TenseMode tense) {
        return switch (tense) {
            case PRESENT -> isPlural ? "are" : "is";
            case PAST -> isPlural ? "were" : "was";
            case FUTURE -> "will be";
            case CONDITIONAL -> "might be";
            case URGENT -> isPlural ? "are" : "is";
            case REQUEST -> "should be";
            case DECISION -> "decided to be";
            case QUESTION -> isPlural ? "are" : "is";
            case TRADE -> isPlural ? "are offering" : "is offering";
            case GOAL -> "'s goal is to be";
        };
    }
    
    /**
     * Conjugate verb based on subject and tense.
     */
    private static String conjugateVerb(String verb, String subject, boolean negated, TenseMode tense) {
        if (verb == null || verb.isEmpty()) return "";
        
        return switch (tense) {
            case PRESENT -> {
                // Special verbs use simple present, not progressive
                if (verb.equals("surrender") || verb.equals("accept")) {
                    if (negated) yield "does not " + verb;
                    yield verb + "s"; // "surrenders", "accepts"
                }
                if (negated) yield "does not " + verb;
                yield "is " + getIngForm(verb); // Progressive: "is building", "is dying"
            }
            case PAST -> {
                if (negated) yield "did not " + verb;
                String pastForm = verb.endsWith("e") ? verb + "d" : verb + "ed";
                yield pastForm; // "attacked", "traded"
            }
            case FUTURE -> {
                if (negated) yield "will not " + verb;
                yield "will " + verb; // "will attack"
            }
            case CONDITIONAL -> {
                if (negated) yield "might not " + verb;
                yield "might " + verb; // "might attack"
            }
            case URGENT -> {
                // Use normal present progressive like PRESENT tense
                if (negated) yield "is not " + getIngForm(verb);
                yield "is " + getIngForm(verb); // "is attacking"
            }
            case REQUEST -> {
                String requestVerb = verb.equals("gear") ? "gear up" : verb;
                if (negated) yield "should not " + requestVerb;
                yield "should " + requestVerb; // "should attack" or "should gear up"
            }
            case DECISION -> {
                if (negated) yield "decided not to " + verb;
                yield "decided to " + verb; // "decided to attack"
            }
            case GOAL -> {
                yield "'s goal is to " + verb; // "goal is to attack"
            }
            case TRADE -> {
                if (negated) yield "does not offer";
                yield "offers"; // Trade specific
            }
            default -> verb;
        };
    }
    
    private static String toAgentNoun(String base) {
        if (base == null || base.isEmpty()) return base;
        
        if (base.equals("raid")) return "raider";
        if (base.equals("attack")) return "attacker";
        if (base.equals("build")) return "builder";
        if (base.equals("mine")) return "miner";
        if (base.equals("trade")) return "trader";
        if (base.equals("library")) return "librarian";
        
        if (base.equals("diplomat") || base.equals("trader") || base.equals("librarian") || 
            base.equals("builder") || base.equals("miner") || base.equals("raider") || base.equals("attacker") ||
            base.equals("blacksmith") || base.equals("cartographer")) {
            return base;
        }
        
        if (base.endsWith("e")) return base + "r";
        return base + "er";
    }
    
    private static String toVerbForm(String word) {
        if (word.endsWith("er")) {
            String base = word.substring(0, word.length() - 2);
            if (CarniteConstants.isVerb(base)) return base;
        }
        if (CarniteConstants.isVerb(word)) return word;
        return null;
    }
    
    private static String formatList(List<String> items) {
        if (items.isEmpty()) return "";
        if (items.size() == 1) return items.get(0);
        if (items.size() == 2) return items.get(0) + " and " + items.get(1);
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            sb.append(items.get(i));
            if (i < items.size() - 2) {
                sb.append(", ");
            } else if (i == items.size() - 2) {
                sb.append(", and ");
            }
        }
        return sb.toString();
    }
    
    private static List<String> extractComponents(List<CarniteParser.CarniteToken> tokens) {
        List<String> components = new ArrayList<>();
        for (CarniteParser.CarniteToken token : tokens) {
            if (token.type() == CarniteParser.CarniteTokenType.WORD) {
                components.add(token.value());
            }
        }
        return components;
    }
    
    public enum TenseMode {
        PRESENT,      // White - happening now
        PAST,         // Light Gray - already happened
        FUTURE,       // Gray - will happen
        CONDITIONAL,  // Pink - might happen
        URGENT,       // Red - EMERGENCY
        REQUEST,      // Light Blue - should do
        DECISION,     // Black - decided to do
        QUESTION,     // Blue - asking
        TRADE,        // Yellow - offering trade
        GOAL          // Purple - goal is to
    }
    
    public enum PatternType {
        TRADE_OFFER,
        QUESTION,
        YES_NO_QUESTION,
        RESPONSE,
        STATEMENT
    }
    
    public record MessagePattern(PatternType type, int confidence) {}
    
    public record TranslationResult(
        String translation,
        List<String> components,
        String patternType
    ) {}
}
