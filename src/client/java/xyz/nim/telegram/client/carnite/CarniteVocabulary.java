package xyz.nim.telegram.client.carnite;

import java.util.*;

public class CarniteVocabulary {
    
    private static final Map<String, String> COMMON_ABBREVIATIONS = new HashMap<>();
    private static final Map<String, String> SYMBOLS = new LinkedHashMap<>();
    private static final List<String> BANNER_COLORS = new ArrayList<>();
    
    static {
        // Common abbreviations
        COMMON_ABBREVIATIONS.put("atk", "attack");
        COMMON_ABBREVIATIONS.put("rd", "raid");
        COMMON_ABBREVIATIONS.put("rdrs", "raiders");
        COMMON_ABBREVIATIONS.put("dp", "diplomat");
        COMMON_ABBREVIATIONS.put("bld", "builder");
        COMMON_ABBREVIATIONS.put("mn", "miner");
        COMMON_ABBREVIATIONS.put("smth", "blacksmith");
        COMMON_ABBREVIATIONS.put("lib", "librarian");
        COMMON_ABBREVIATIONS.put("crt", "cartographer");
        COMMON_ABBREVIATIONS.put("tdr", "trader");
        COMMON_ABBREVIATIONS.put("heal", "healer");
        COMMON_ABBREVIATIONS.put("grd", "guardsman");
        
        // Resources
        COMMON_ABBREVIATIONS.put("dmd", "diamond");
        COMMON_ABBREVIATIONS.put("irn", "iron");
        COMMON_ABBREVIATIONS.put("gpdr", "gunpowder");
        COMMON_ABBREVIATIONS.put("brd", "bread");
        COMMON_ABBREVIATIONS.put("fd", "food");
        COMMON_ABBREVIATIONS.put("bndg", "bandage");
        COMMON_ABBREVIATIONS.put("ench", "enchant");
        COMMON_ABBREVIATIONS.put("swd", "sword");
        COMMON_ABBREVIATIONS.put("acft", "autocrafter");
        
        // Actions
        COMMON_ABBREVIATIONS.put("mov", "move");
        COMMON_ABBREVIATIONS.put("ally", "ally");
        COMMON_ABBREVIATIONS.put("trd", "trade");
        COMMON_ABBREVIATIONS.put("die", "die");
        COMMON_ABBREVIATIONS.put("kill", "kill");
        COMMON_ABBREVIATIONS.put("take", "steal");
        COMMON_ABBREVIATIONS.put("get", "receive");
        COMMON_ABBREVIATIONS.put("lost", "lost");
        COMMON_ABBREVIATIONS.put("merg", "merge");
        COMMON_ABBREVIATIONS.put("elct", "elect");
        COMMON_ABBREVIATIONS.put("wtd", "wanted");
        COMMON_ABBREVIATIONS.put("acpt", "accept");
        COMMON_ABBREVIATIONS.put("srd", "surrender");
        COMMON_ABBREVIATIONS.put("mtgm", "metagaming");
        COMMON_ABBREVIATIONS.put("gear", "gear up");
        COMMON_ABBREVIATIONS.put("call", "call");
        COMMON_ABBREVIATIONS.put("try", "try");
        
        // Adjectives
        COMMON_ABBREVIATIONS.put("blss", "blessed");
        COMMON_ABBREVIATIONS.put("scrt", "secret");
        COMMON_ABBREVIATIONS.put("dngr", "dangerous");
        
        // Locations/Events
        COMMON_ABBREVIATIONS.put("metng", "meeting");
        COMMON_ABBREVIATIONS.put("elctn", "election");
        COMMON_ABBREVIATIONS.put("evnt", "event");
        COMMON_ABBREVIATIONS.put("vst", "visitor");
        COMMON_ABBREVIATIONS.put("arsn", "arson");
        
        // Time
        COMMON_ABBREVIATIONS.put("t", "time");
        COMMON_ABBREVIATIONS.put("m", "minutes");
        COMMON_ABBREVIATIONS.put("h", "hours");
        
        // Symbols
        SYMBOLS.put("|", "Agent/Individual - after noun to indicate a player");
        SYMBOLS.put(":", "Your civ/To - addressing another civ");
        SYMBOLS.put(";", "My civ/We/Us - referring to speaker's civ");
        SYMBOLS.put(",", "Property/Of - links descriptors to nouns");
        SYMBOLS.put("&", "And - joins two related items");
        SYMBOLS.put(".", "Stack (64 items) - 2.5 means 2 stacks + 5 items");
        SYMBOLS.put("_", "Question blank - what information is being asked");
        SYMBOLS.put("^", "Response/Because - links to previous statement");
        SYMBOLS.put("''", "Quote - addresses specific term/phrase");
        SYMBOLS.put("~", "Plural/About/Around - ~5 means 'about 5'");
        SYMBOLS.put("::", "All civs/To everyone on channel");
        SYMBOLS.put("-", "Negation/Not - placed before negated term");
        
        // Banner colors
        BANNER_COLORS.add("White - Present Statement");
        BANNER_COLORS.add("Light Gray - Past Statement");
        BANNER_COLORS.add("Gray - Future Statement");
        BANNER_COLORS.add("Pink - Conditional/Might");
        BANNER_COLORS.add("Red - Urgent/High Priority");
        BANNER_COLORS.add("Light Blue - Request/Command");
        BANNER_COLORS.add("Black - Opinion/Decision");
        BANNER_COLORS.add("Blue - Y/N Question");
        BANNER_COLORS.add("Yellow - Trade Offer");
        BANNER_COLORS.add("Purple - Goal/Objective");
    }
    
    public static String expand(String abbreviation) {
        return COMMON_ABBREVIATIONS.getOrDefault(abbreviation.toLowerCase(), abbreviation);
    }
    
    public static String abbreviate(String word) {
        for (Map.Entry<String, String> entry : COMMON_ABBREVIATIONS.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(word)) {
                return entry.getKey();
            }
        }
        
        // Auto-abbreviate by removing vowels
        String result = word.toLowerCase().replaceAll("[aeiou]", "");
        if (result.length() > 4) {
            result = result.substring(0, 4);
        } else if (result.length() < 2) {
            result = word.substring(0, Math.min(3, word.length()));
        }
        return result;
    }
    
    public static Map<String, String> getAllAbbreviations() {
        return new HashMap<>(COMMON_ABBREVIATIONS);
    }
    
    public static Map<String, String> getSymbols() {
        return new LinkedHashMap<>(SYMBOLS);
    }
    
    public static List<String> getBannerColors() {
        return new ArrayList<>(BANNER_COLORS);
    }
    
    public static List<String> getAutocompleteSuggestions(String partial) {
        List<String> suggestions = new ArrayList<>();
        String lower = partial.toLowerCase();
        
        for (String abbr : COMMON_ABBREVIATIONS.keySet()) {
            if (abbr.startsWith(lower)) {
                suggestions.add(abbr + " (" + COMMON_ABBREVIATIONS.get(abbr) + ")");
            }
        }
        
        return suggestions;
    }
    
    public static String formatWithExpansion(String message) {
        StringBuilder result = new StringBuilder();
        String[] words = message.split(" ");
        
        for (String word : words) {
            String clean = word.replaceAll("[^a-zA-Z]", "");
            String expanded = expand(clean);
            
            if (!expanded.equals(clean)) {
                result.append(word).append(" [").append(expanded).append("] ");
            } else {
                result.append(word).append(" ");
            }
        }
        
        return result.toString().trim();
    }
}
