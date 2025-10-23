package xyz.nim.telegraph.client.carnite;

import java.util.*;

public class CarniteVocabulary {
    
    private static final Map<String, String> COMMON_ABBREVIATIONS = new HashMap<>();
    private static final Map<String, String> SYMBOLS = new LinkedHashMap<>();
    private static final List<String> BANNER_COLORS = new ArrayList<>();
    
    // User-configurable civilization registry
    // Admins should populate this with their server's civilization names
    private static final Map<String, String> CIVILIZATION_REGISTRY = new HashMap<>();
    
    static {
        // Civilizations are now managed through the settings UI
        // No default civilizations - users add their own
        
        // Common abbreviations
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
        COMMON_ABBREVIATIONS.put("gld", "gold");
        COMMON_ABBREVIATIONS.put("emrld", "emerald");
        COMMON_ABBREVIATIONS.put("cl", "coal");
        COMMON_ABBREVIATIONS.put("cppr", "copper");
        COMMON_ABBREVIATIONS.put("rdst", "redstone");
        COMMON_ABBREVIATIONS.put("lps", "lapis");
        COMMON_ABBREVIATIONS.put("qrtz", "quartz");
        COMMON_ABBREVIATIONS.put("obsd", "obsidian");
        COMMON_ABBREVIATIONS.put("nthr", "netherite");
        COMMON_ABBREVIATIONS.put("prl", "pearl");
        COMMON_ABBREVIATIONS.put("endr", "ender");
        
        // Food
        COMMON_ABBREVIATIONS.put("brd", "bread");
        COMMON_ABBREVIATIONS.put("fd", "food");
        
        // Combat & Tools
        COMMON_ABBREVIATIONS.put("gpdr", "gunpowder");
        COMMON_ABBREVIATIONS.put("bndg", "bandage");
        COMMON_ABBREVIATIONS.put("ench", "enchant");
        COMMON_ABBREVIATIONS.put("swd", "sword");
        COMMON_ABBREVIATIONS.put("bow", "bow");
        COMMON_ABBREVIATIONS.put("xbow", "crossbow");
        COMMON_ABBREVIATIONS.put("arrw", "arrow");
        COMMON_ABBREVIATIONS.put("armr", "armor");
        COMMON_ABBREVIATIONS.put("ptn", "potion");
        COMMON_ABBREVIATIONS.put("heal", "healing");
        COMMON_ABBREVIATIONS.put("ttm", "totem");
        COMMON_ABBREVIATIONS.put("eltr", "elytra");
        COMMON_ABBREVIATIONS.put("shld", "shield");
        
        // Armor pieces
        COMMON_ABBREVIATIONS.put("hlmt", "helmet");
        COMMON_ABBREVIATIONS.put("chst", "chestplate");
        COMMON_ABBREVIATIONS.put("lgs", "leggings");
        COMMON_ABBREVIATIONS.put("bts", "boots");
        
        // Tools
        COMMON_ABBREVIATIONS.put("pck", "pickaxe");
        COMMON_ABBREVIATIONS.put("ax", "axe");
        COMMON_ABBREVIATIONS.put("shvl", "shovel");
        COMMON_ABBREVIATIONS.put("ho", "hoe");
        COMMON_ABBREVIATIONS.put("shrs", "shears");
        
        // Blocks
        COMMON_ABBREVIATIONS.put("cbbl", "cobblestone");
        COMMON_ABBREVIATIONS.put("stn", "stone");
        COMMON_ABBREVIATIONS.put("wd", "wood");
        COMMON_ABBREVIATIONS.put("wl", "wool");
        COMMON_ABBREVIATIONS.put("lg", "log");
        COMMON_ABBREVIATIONS.put("plnk", "plank");
        COMMON_ABBREVIATIONS.put("stck", "stick");
        COMMON_ABBREVIATIONS.put("glw", "glowstone");
        
        // Misc items
        COMMON_ABBREVIATIONS.put("bk", "book");
        COMMON_ABBREVIATIONS.put("ppr", "paper");
        COMMON_ABBREVIATIONS.put("lthr", "leather");
        COMMON_ABBREVIATIONS.put("acft", "autocrafter");
        
        // Actions
        COMMON_ABBREVIATIONS.put("mov", "move");
        COMMON_ABBREVIATIONS.put("ally", "ally");
        COMMON_ABBREVIATIONS.put("trd", "trade");
        COMMON_ABBREVIATIONS.put("die", "die");
        COMMON_ABBREVIATIONS.put("kill", "kill");
        COMMON_ABBREVIATIONS.put("take", "steal");
        COMMON_ABBREVIATIONS.put("get", "get");
        COMMON_ABBREVIATIONS.put("lost", "lost");
        COMMON_ABBREVIATIONS.put("merg", "merge");
        COMMON_ABBREVIATIONS.put("elct", "elect");
        COMMON_ABBREVIATIONS.put("wtd", "wanted");
        COMMON_ABBREVIATIONS.put("acpt", "accept");
        COMMON_ABBREVIATIONS.put("srd", "surrender");
        COMMON_ABBREVIATIONS.put("mtgm", "metagaming");
        COMMON_ABBREVIATIONS.put("gear", "gear");
        COMMON_ABBREVIATIONS.put("call", "call");
        COMMON_ABBREVIATIONS.put("try", "try");
        COMMON_ABBREVIATIONS.put("snd", "send");
        
        // Objects/Things
        COMMON_ABBREVIATIONS.put("frm", "farm");
        
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
        // Check if it's a civilization code (2-4 uppercase letters)
        if (abbreviation.matches("[A-Z]{2,4}")) {
            return CIVILIZATION_REGISTRY.getOrDefault(abbreviation, abbreviation);
        }
        return COMMON_ABBREVIATIONS.getOrDefault(abbreviation.toLowerCase(), abbreviation);
    }
    
    /**
     * Register a civilization with its abbreviation.
     * Server admins should call this during initialization.
     */
    public static void registerCivilization(String code, String fullName) {
        CIVILIZATION_REGISTRY.put(code.toUpperCase(), fullName);
    }
    
    /**
     * Get the full name of a civilization from its code.
     */
    public static String getCivilizationName(String code) {
        return CIVILIZATION_REGISTRY.getOrDefault(code.toUpperCase(), code);
    }
    
    /**
     * Check if a code is a registered civilization.
     */
    public static boolean isCivilization(String code) {
        return code.matches("[A-Z]{2,4}") && CIVILIZATION_REGISTRY.containsKey(code.toUpperCase());
    }
    
    /**
     * Remove a civilization from the registry.
     */
    public static void removeCivilization(String code) {
        CIVILIZATION_REGISTRY.remove(code.toUpperCase());
    }
    
    /**
     * Get all registered civilizations.
     */
    public static Map<String, String> getAllCivilizations() {
        return new HashMap<>(CIVILIZATION_REGISTRY);
    }
    
    /**
     * Clear all civilizations (for testing/reset).
     */
    public static void clearCivilizations() {
        CIVILIZATION_REGISTRY.clear();
    }
    
    /**
     * Load civilizations from a map (for persistence).
     */
    public static void loadCivilizations(Map<String, String> civilizations) {
        CIVILIZATION_REGISTRY.clear();
        CIVILIZATION_REGISTRY.putAll(civilizations);
    }
    
    public static String abbreviate(String word) {
        word = word.trim();
        
        // Handle compound terms (adjective + noun) like "blessed food"
        if (word.contains(" ")) {
            String[] parts = word.split("\\s+");
            if (parts.length == 2) {
                String adj = abbreviateSingleWord(parts[0]);
                String noun = abbreviateSingleWord(parts[1]);
                return adj + "," + noun;
            } else if (parts.length > 2) {
                // Multiple words - abbreviate each
                StringBuilder result = new StringBuilder();
                for (int i = 0; i < parts.length - 1; i++) {
                    result.append(abbreviateSingleWord(parts[i])).append(",");
                }
                result.append(abbreviateSingleWord(parts[parts.length - 1]));
                return result.toString();
            }
        }
        
        // Single word
        return abbreviateSingleWord(word);
    }
    
    private static String abbreviateSingleWord(String word) {
        // Check vocabulary first
        for (Map.Entry<String, String> entry : COMMON_ABBREVIATIONS.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(word)) {
                return entry.getKey();
            }
        }
        
        // Auto-abbreviate by removing vowels
        String result = word.toLowerCase().replaceAll("[aeiou]", "");
        if (result.isEmpty()) {
            result = word.toLowerCase();
        }
        if (result.length() > 4) {
            result = result.substring(0, 4);
        } else if (result.length() < 2 && word.length() >= 2) {
            result = word.substring(0, Math.min(3, word.length())).toLowerCase();
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
