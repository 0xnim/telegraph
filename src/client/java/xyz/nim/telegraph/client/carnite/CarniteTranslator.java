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
    
    // Mass nouns that should never be pluralized in Carnite
    // Based on test expectations: bread, iron stay singular; diamond, bandage get pluralized
    private static final Set<String> MASS_NOUNS = Set.of(
        "bread", "iron", "gold", "gunpowder", "food", "wood", "stone", "enchant"
    );
    
    public static TranslationResult translate(String message, String bannerColor) {
        if (message == null || message.isEmpty()) {
            return new TranslationResult("", Collections.emptyList(), "");
        }
        
        CarniteParser.ParsedCarniteMessage parsed = CarniteParser.parse(message, bannerColor);
        TenseMode tenseMode = getTenseModeFromColor(bannerColor);
        
        // Detect message pattern
        MessagePattern pattern = detectPattern(message, parsed.tokens());
        
        // Translate based on pattern
        String translation = switch (pattern.type()) {
            case TRADE_OFFER -> translateTradeOffer(parsed.tokens(), tenseMode, message);
            case QUESTION -> translateQuestion(parsed.tokens(), tenseMode);
            case RESPONSE -> translateResponse(parsed.tokens(), tenseMode);
            case STATEMENT -> translateStatement(parsed.tokens(), tenseMode);
        };
        
        List<String> components = extractComponents(parsed.tokens());
        
        return new TranslationResult(translation, components, pattern.type().name());
    }
    
    private static TenseMode getTenseModeFromColor(String bannerColor) {
        if (bannerColor == null) return TenseMode.PRESENT;
        
        if (bannerColor.contains("white")) return TenseMode.PRESENT;
        if (bannerColor.contains("light_gray")) return TenseMode.PAST;
        if (bannerColor.contains("dark_gray") || (bannerColor.contains("gray") && !bannerColor.contains("light"))) return TenseMode.FUTURE;
        if (bannerColor.contains("pink")) return TenseMode.CONDITIONAL;
        if (bannerColor.contains("red")) return TenseMode.URGENT;
        if (bannerColor.contains("light_blue")) return TenseMode.REQUEST;
        if (bannerColor.contains("black")) return TenseMode.DECISION;
        if (bannerColor.contains("blue") && !bannerColor.contains("light")) return TenseMode.QUESTION;
        if (bannerColor.contains("yellow")) return TenseMode.TRADE;
        if (bannerColor.contains("purple") || bannerColor.contains("magenta")) return TenseMode.GOAL;
        
        return TenseMode.PRESENT;
    }
    
    private static MessagePattern detectPattern(String message, List<CarniteParser.CarniteToken> tokens) {
        boolean hasSemicolon = tokens.stream().anyMatch(t -> t.type() == CarniteParser.CarniteTokenType.MY_CIV);
        boolean hasColon = tokens.stream().anyMatch(t -> t.type() == CarniteParser.CarniteTokenType.YOUR_CIV);
        boolean hasQuestion = tokens.stream().anyMatch(t -> t.type() == CarniteParser.CarniteTokenType.QUESTION_BLANK);
        boolean hasResponse = tokens.stream().anyMatch(t -> t.type() == CarniteParser.CarniteTokenType.RESPONSE);
        
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
        
        for (int i = 0; i < tokens.size(); i++) {
            CarniteParser.CarniteToken token = tokens.get(i);
            
            if (token.type() == CarniteParser.CarniteTokenType.MY_CIV) {
                beforeMyCiv = false;
                continue;
            }
            
            if (token.type() == CarniteParser.CarniteTokenType.YOUR_CIV || 
                token.type() == CarniteParser.CarniteTokenType.QUESTION_BLANK) {
                continue;
            }
            
            if (beforeMyCiv) {
                if (token.type() == CarniteParser.CarniteTokenType.STACK && 
                    i + 1 < tokens.size() && tokens.get(i + 1).type() == CarniteParser.CarniteTokenType.WORD) {
                    // . followed by item = .dmd = 64 diamonds
                    String item = tokens.get(i + 1).value();
                    String itemName = CarniteVocabulary.expand(item.replaceAll("\\d+", ""));
                    // Pluralize count nouns
                    if (!MASS_NOUNS.contains(itemName)) {
                        itemName = pluralize(itemName);
                    }
                    offerings.add("64 " + itemName);
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
        
        result.append(". What will you give in return?");
        
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
                    
                    // Check for special question markers
                    // Note: _t, _rsn get tokenized with _ as QUESTION_BLANK, so check just "t" and "rsn"
                    if (word.equals("t") && questionPos >= 0) {
                        questionWord = "When";
                        continue;
                    }
                    if (word.equals("rsn") && questionPos >= 0) {
                        questionWord = "Why";
                        continue;
                    }
                    // Check if word starts with _ (for _dmd pattern)
                    if (word.startsWith("_")) {
                        String item = word.substring(1);
                        questionItem = CarniteVocabulary.expand(item);
                        questionWord = "How many";
                        continue;
                    }
                    // Also check if previous token was _, making this an item question
                    if (questionPos == i - 1 && !word.matches("[A-Z]{2,4}") && !word.equals("t") && !word.equals("rsn")) {
                        questionItem = CarniteVocabulary.expand(word);
                        questionWord = "How many";
                        continue;
                    }
                    
                    // Check if civ
                    if (word.matches("[A-Z]{2,4}")) {
                        locations.add(CarniteVocabulary.getCivilizationName(word));
                        continue;
                    }
                    
                    // Check if agent
                    if (i + 1 < tokens.size() && tokens.get(i + 1).type() == CarniteParser.CarniteTokenType.AGENT) {
                        String agentPhrase = parseAgentPhrase(word, hasPlural);
                        agents.add(agentPhrase);
                        hasPlural = false;
                        i++; // Skip agent marker
                        continue;
                    }
                    
                    // Check if verb
                    String verbForm = toVerbForm(expanded);
                    if (verbForm != null || isVerb(expanded)) {
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
                questionWord = "What";
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
            result.append(" ").append(questionItem);
        }
        
        // For "What" object questions, word order is: What did [agent] [verb]
        // For "Who" questions, word order is: Who [verb-past] [object]
        // For other questions: [Question] did [agent] [verb]
        
        if (verb != null && questionWord.equals("What") && questionPos == 0) {
            // What did X do? pattern
            result.append(" did");
        }
        
        // Add agents (subject)
        if (!agents.isEmpty()) {
            String agent = agents.get(0);
            // Add "the" for plural agents (except for "What" questions)
            if (agent.toLowerCase().startsWith("some ")) {
                if (questionWord.equals("What")) {
                    // "What" questions: no article
                    agent = agent.substring(5);
                } else {
                    // Other questions: use "the"
                    agent = "the " + agent.substring(5);
                }
            }
            result.append(" ").append(agent);
        }
        
        // Add verb
        if (verb != null) {
            if (isAgentQuestion || questionWord.equals("Which civ/What")) {
                // Who questions: use past tense directly
                String pastForm = getIrregularPast(verb);
                result.append(" ").append(pastForm);
            } else if (questionWord.equals("What") && questionPos == 0) {
                // "What" object questions: already added "did", now just base form
                result.append(" ").append(verb);
            } else {
                // All other questions (When/Where/Why/How): use "did" + base form
                result.append(" did ").append(verb);
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
        
        for (int i = 0; i < tokens.size(); i++) {
            CarniteParser.CarniteToken token = tokens.get(i);
            
            switch (token.type()) {
                case PLURAL -> hasPlural = true;
                case NEGATION -> hasNegation = true;
                case AGENT -> hasAgent = true;
                case PROPERTY -> {} // Skip - handled when building noun phrases
                
                case MY_CIV -> {
                    // MY_CIV always marks "My civilization" as the subject (when verb follows) or location
                    // If previous token was a civ (CIV;), that civ is in directObjects already as Od
                    
                    // Check if there's a verb later - if so, this is the subject
                    boolean hasVerbLater = tokens.stream().skip(i + 1).anyMatch(t -> {
                        if (t.type() == CarniteParser.CarniteTokenType.WORD) {
                            String exp = CarniteVocabulary.expand(t.value());
                            return isVerb(exp) || toVerbForm(exp) != null;
                        }
                        return false;
                    });
                    
                    if (hasVerbLater) {
                        subject = "My civilization";
                    } else {
                        indirectObjects.add("my civilization");
                    }
                }
                
                case YOUR_CIV -> {
                    // YOUR_CIV marks destination: previous WORD was civ name
                    // Look back for the civ code
                    if (i > 0) {
                        for (int j = i - 1; j >= 0; j--) {
                            if (tokens.get(j).type() == CarniteParser.CarniteTokenType.WORD) {
                                String civCode = tokens.get(j).value();
                                if (civCode.matches("[A-Z]{2,4}")) {
                                    String civName = CarniteVocabulary.getCivilizationName(civCode);
                                    // Remove from subject/indirectObjects if added
                                    indirectObjects.remove(civName);
                                    if (subject != null && subject.equals(civName)) {
                                        subject = null;
                                    }
                                    indirectObjects.add("to (you) " + civName);
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
                        
                        int total = stackCount * 64 + remainder;
                        
                        // Pluralize count nouns
                        if (!MASS_NOUNS.contains(itemName) && total > 1) {
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
                    
                    // Check if civ code
                    if (word.matches("[A-Z]{2,4}")) {
                        String civName = CarniteVocabulary.getCivilizationName(word);
                        
                        // Look ahead for markers (not just immediate next token)
                        int k = i + 1;
                        boolean followedByMyCiv = false;
                        boolean followedByYourCiv = false;
                        
                        while (k < tokens.size()) {
                            var t = tokens.get(k).type();
                            if (t == CarniteParser.CarniteTokenType.MY_CIV) {
                                followedByMyCiv = true;
                                break;
                            }
                            if (t == CarniteParser.CarniteTokenType.YOUR_CIV) {
                                followedByYourCiv = true;
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
                        
                        if (followedByYourCiv) {
                            // CIV: - will be handled in YOUR_CIV case, skip
                            continue;
                        } else if (followedByMyCiv) {
                            // CIV; - This civ is the direct object, MY_CIV will set subject
                            directObjects.add(civName);
                            continue;
                        } else {
                            // Civ without marker: determine role based on context
                            if (subject == null) {
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
                    }
                    
                    // Check if followed by agent marker FIRST (before checking if verb)
                    // This prevents "rd|" from being treated as verb "raid"
                    boolean willBeAgent = i + 1 < tokens.size() && 
                        tokens.get(i + 1).type() == CarniteParser.CarniteTokenType.AGENT;
                    
                    if (hasAgent || willBeAgent) {
                        // This is an agent noun - NOT a verb
                        String agentPhrase = parseAgentPhrase(word, hasPlural);
                        directObjects.add(agentPhrase);
                        hasPlural = false;
                        hasAgent = false;
                        if (willBeAgent) i++; // Skip agent marker
                        continue;
                    }
                    
                    // Check if this could be a verb
                    // Priority: if we have subject, next word is likely verb
                    String verbForm = toVerbForm(expanded);
                    if (verbForm != null && (subject != null || !indirectObjects.isEmpty())) {
                        // We have S or Oi, so this is likely V
                        verb = verbForm;
                        continue;
                    } else if (isVerb(expanded)) {
                        verb = expanded;
                        continue;
                    }
                    
                    // Check if followed by property marker to build full phrase
                    String fullWord = word;
                    if (i + 1 < tokens.size() && tokens.get(i + 1).type() == CarniteParser.CarniteTokenType.PROPERTY) {
                        // Collect all parts: blss , fd → "blss,fd"
                        StringBuilder propertyPhrase = new StringBuilder(word);
                        while (i + 1 < tokens.size() && 
                               (tokens.get(i + 1).type() == CarniteParser.CarniteTokenType.PROPERTY ||
                                tokens.get(i + 1).type() == CarniteParser.CarniteTokenType.WORD)) {
                            i++;
                            CarniteParser.CarniteToken next = tokens.get(i);
                            if (next.type() == CarniteParser.CarniteTokenType.PROPERTY) {
                                propertyPhrase.append(",");
                            } else {
                                propertyPhrase.append(next.value());
                                break; // End after the property value
                            }
                        }
                        fullWord = propertyPhrase.toString();
                    }
                    
                    // Try to parse as noun phrase (Od)
                    String nounPhrase = parseNounPhrase(fullWord, hasPlural);
                    if (!nounPhrase.isEmpty()) {
                        directObjects.add(nounPhrase);
                        hasPlural = false;
                    }
                }
            }
        }
        
        // Build English sentence
        return constructSentence(directObjects, indirectObjects, subject, verb, hasNegation, tense);
    }
    
    /**
     * Construct an English sentence from parsed Od Oi S V components.
     */
    private static String constructSentence(List<String> directObjects, List<String> indirectObjects,
                                           String subject, String verb, boolean hasNegation, TenseMode tense) {
        StringBuilder result = new StringBuilder();
        
        // Add tense prefix (but not for DECISION - that's handled in verb conjugation)
        if (tense == TenseMode.URGENT) {
            result.append("⚠ URGENT: ");
        } else if (tense == TenseMode.GOAL) {
            result.append("My nation's current goal is to ");
        }
        
        // Simple case: just indirect object with no Od/V (e.g., ";" alone or "CN:" alone)
        if (directObjects.isEmpty() && verb == null && subject == null && !indirectObjects.isEmpty()) {
            String ioFirst = indirectObjects.get(0);
            if (ioFirst.equals("my civilization")) {
                return "My civilization.";
            } else if (ioFirst.startsWith("to ")) {
                return ioFirst.substring(3) + ".";
            }
            return capitalize(ioFirst) + ".";
        }
        
        // Case: Od Oi with no explicit subject/verb (e.g., "~rd| ;" = "some raiders at my civ")
        if (!directObjects.isEmpty() && !indirectObjects.isEmpty() && subject == null && verb == null) {
            String od = directObjects.get(0);
            String oi = indirectObjects.get(0);
            
            result.append(capitalize(od));
            
            // Determine verb "is" or "are" based on plurality
            if (od.toLowerCase().startsWith("some") || od.toLowerCase().contains(" and ")) {
                result.append(" are");
            } else {
                result.append(" is");
            }
            
            // Determine preposition
            if (oi.equals("my civilization")) {
                result.append(" at ").append(oi);
            } else if (oi.startsWith("to ")) {
                result.append(" at ").append(oi.substring(3));
            } else {
                result.append(" at ").append(oi);
            }
            
            result.append(tense == TenseMode.URGENT ? "!" : ".");
            return result.toString();
        }
        
        // Case: Subject + Verb (e.g., "; mov" = "my civ might move")
        if (subject == null && verb != null && !indirectObjects.isEmpty() && indirectObjects.get(0).equals("my civilization")) {
            result.append("My civilization");
            result.append(" ");
            result.append(conjugateVerb(verb, "My civilization", hasNegation, tense));
            
            if (!directObjects.isEmpty()) {
                result.append(" ").append(formatList(directObjects));
            }
            
            // Special case: conditional gets "Undecided." suffix
            if (tense == TenseMode.CONDITIONAL) {
                result.append(". Undecided");
            }
            
            result.append(".");
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
            result.append(subject);
            result.append(" ");
            result.append(conjugateVerb(verb, subject, hasNegation, tense));
            
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
                        if (verb.equals("surrender") && !io.contains("(you)")) {
                            result.append(" to (you) ").append(io);
                        } else {
                            result.append(" ").append(preposition).append(" ").append(io);
                        }
                    } else if (io.equals("my civilization")) {
                        // Skip - this was the subject marker
                    } else if (io.startsWith("to ")) {
                        // Already has "to" prefix (possibly with "(you)")
                        result.append(" ").append(io);
                    } else {
                        result.append(" ").append(io);
                    }
                }
            }
            
            result.append(".");
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
        
        // Case: Just objects (no subject/verb)
        if (!directObjects.isEmpty()) {
            result.append(capitalize(formatList(directObjects))).append(".");
            return result.toString();
        }
        
        // Fallback
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
        // Handle simple numbers with items: 32irn, 2bld, etc.
        Pattern numPattern = Pattern.compile("(\\d+)([a-z,]+)");
        Matcher matcher = numPattern.matcher(word);
        
        if (matcher.matches()) {
            int count = Integer.parseInt(matcher.group(1));
            String item = matcher.group(2);
            String itemName = expandWithProperties(item);
            // Pluralize count nouns (non-mass nouns) when count > 1
            if (count > 1 && !MASS_NOUNS.contains(itemName)) {
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
     * Parse a noun phrase with quantity, stacks, and properties.
     * Mass nouns are never pluralized.
     */
    private static String parseNounPhrase(String word, boolean hasPlural) {
        // Handle stack notation: 2.5dmd, 3.32brd, etc.
        Pattern stackPattern = Pattern.compile("(\\d+)\\.(\\d*)([a-z,]+)");
        Matcher matcher = stackPattern.matcher(word);
        
        if (matcher.matches()) {
            int stacks = Integer.parseInt(matcher.group(1));
            String remainder = matcher.group(2);
            String item = matcher.group(3);
            String itemName = expandWithProperties(item);
            
            int remainderNum = remainder.isEmpty() ? 0 : Integer.parseInt(remainder);
            int total = stacks * 64 + remainderNum;
            
            // Mass nouns never get pluralized
            return total + " " + itemName + " (" + stacks + " stacks" + 
                   (remainderNum > 0 ? " + " + remainderNum : "") + ")";
        }
        
        // Handle simple numbers with items: 32irn, 2bld, etc.
        Pattern numPattern = Pattern.compile("(\\d+)([a-z,]+)");
        matcher = numPattern.matcher(word);
        
        if (matcher.matches()) {
            int count = Integer.parseInt(matcher.group(1));
            String item = matcher.group(2);
            String itemName = expandWithProperties(item);
            // Mass nouns never get pluralized
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
            // Add article for singular nouns
            if (hasPlural) {
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
    private static String parseAgentPhrase(String word, boolean hasPlural) {
        // Handle pure numeric agents: 170| = "player 170"
        if (word.matches("\\d+")) {
            return "player " + word;
        }
        
        // Handle numeric agents with roles: 2bld| = "2 builders"
        Pattern numPattern = Pattern.compile("(\\d+)([a-z,]+)");
        Matcher matcher = numPattern.matcher(word);
        
        if (matcher.matches()) {
            int count = Integer.parseInt(matcher.group(1));
            String role = matcher.group(2);
            String roleName = expandWithProperties(role);
            String agentNoun = toAgentNoun(roleName);
            return count + " " + (count > 1 ? agentNoun + "s" : agentNoun);
        }
        
        // Handle plural agents: ~rd| = "some raiders"
        String expanded = CarniteVocabulary.expand(word);
        String agentNoun = toAgentNoun(expanded);
        
        if (hasPlural) {
            return "Some " + agentNoun + "s";
        }
        
        return "A " + agentNoun;
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
                yield "is " + verb + "ing"; // Progressive: "is building"
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
                if (negated) yield "IS NOT " + verb.toUpperCase() + "ING";
                yield "IS " + verb.toUpperCase() + "ING"; // "IS ATTACKING"
            }
            case REQUEST -> {
                if (negated) yield "should not " + verb;
                yield "should " + verb; // "should attack"
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
        
        // Direct agent nouns
        if (base.equals("raid")) return "raider";
        if (base.equals("attack")) return "attacker";
        if (base.equals("build")) return "builder";
        if (base.equals("mine")) return "miner";
        if (base.equals("diplomat")) return "diplomat";
        
        // Simple rule: add "er"
        if (base.endsWith("e")) return base + "r";
        return base + "er";
    }
    
    private static boolean isVerb(String word) {
        String[] verbs = {
            "attack", "raid", "steal", "take", "give", "trade", "receive", 
            "move", "merge", "elect", "die", "kill", "surrender", "accept",
            "ally", "build", "mine", "call", "send", "metagaming"
        };
        
        for (String verb : verbs) {
            if (word.equalsIgnoreCase(verb)) return true;
        }
        return false;
    }
    
    /**
     * Check if a word could be a verb given its base form.
     * Handles cases like "builder" -> "build", "miner" -> "mine"
     */
    private static String toVerbForm(String word) {
        if (word.endsWith("er")) {
            String base = word.substring(0, word.length() - 2);
            if (isVerb(base)) return base;
        }
        if (isVerb(word)) return word;
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
