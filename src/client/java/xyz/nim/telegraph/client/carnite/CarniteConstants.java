package xyz.nim.telegraph.client.carnite;

import java.util.Set;
import java.util.regex.Pattern;

public final class CarniteConstants {
    
    private CarniteConstants() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    public static final int STACK_SIZE = 64;
    
    public static final Pattern CIV_ABBR_PATTERN = Pattern.compile("\\b[A-Z]{2,4}\\b");
    public static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+");
    public static final Pattern NUM_ABBR_PATTERN = Pattern.compile("(\\d+)([a-z,]+)");
    public static final Pattern STACK_PATTERN = Pattern.compile("(\\d+)\\.(\\d*)([a-z,]+)");
    
    public static final Set<String> MASS_NOUNS = Set.of(
        "bread", "iron", "gold", "gunpowder", "food", "wood", "stone", "enchant"
    );
    
    public static final Set<String> VERBS = Set.of(
        "attack", "raid", "steal", "take", "give", "trade", "receive", 
        "move", "merge", "elect", "die", "kill", "surrender", "accept",
        "ally", "build", "mine", "call", "send", "metagaming", "gear", "get"
    );
    
    public static boolean isVerb(String word) {
        return VERBS.contains(word.toLowerCase());
    }
    
    public static boolean isCivAbbreviation(String word) {
        return word.matches("[A-Z]{2,4}");
    }
    
    public static boolean isNumber(String word) {
        return NUMBER_PATTERN.matcher(word).matches();
    }
}
