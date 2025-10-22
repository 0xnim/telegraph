package xyz.nim.telegram.client.carnite;

import java.util.*;

public class CarniteValidator {
    
    public static ValidationResult validate(String message, String bannerColor) {
        List<ValidationIssue> issues = new ArrayList<>();
        List<ValidationSuggestion> suggestions = new ArrayList<>();
        
        if (message == null || message.isEmpty()) {
            issues.add(new ValidationIssue(ValidationSeverity.ERROR, "Message is empty"));
            return new ValidationResult(false, issues, suggestions);
        }
        
        // Check message length
        if (message.length() > 38) {
            issues.add(new ValidationIssue(ValidationSeverity.WARNING, 
                "Message exceeds 38 characters (" + message.length() + "). Text will be very small on map."));
        } else if (message.length() > 32) {
            issues.add(new ValidationIssue(ValidationSeverity.INFO, 
                "Message over 32 characters. Consider shortening for better readability."));
        }
        
        // Check banner color consistency for trade messages
        if (CarniteParser.isTradeMessage(message)) {
            if (bannerColor != null && !bannerColor.contains("yellow")) {
                issues.add(new ValidationIssue(ValidationSeverity.WARNING, 
                    "Trade messages should use YELLOW banners according to Carnite protocol"));
            }
        }
        
        // Check for proper symbol usage
        validateSymbols(message, issues, suggestions);
        
        // Check for civs and agents
        validateCivsAndAgents(message, issues, suggestions);
        
        // Check grammar hints
        validateGrammar(message, issues, suggestions);
        
        boolean isValid = issues.stream().noneMatch(i -> i.severity == ValidationSeverity.ERROR);
        return new ValidationResult(isValid, issues, suggestions);
    }
    
    private static void validateSymbols(String message, List<ValidationIssue> issues, List<ValidationSuggestion> suggestions) {
        // Check for :: without :
        if (message.contains("::") && !message.matches(".*[^:]:[^:].*")) {
            suggestions.add(new ValidationSuggestion(":: means 'to all civs on channel'"));
        }
        
        // Check for agent marker without level
        if (message.matches(".*\\d+[a-z]+\\|(?![0-9]).*")) {
            suggestions.add(new ValidationSuggestion("Consider adding level after agent marker: '2bld|5' = 2 level-5 builders"));
        }
        
        // Check for negation
        if (message.contains("-")) {
            suggestions.add(new ValidationSuggestion("'-' before a term negates it: '-acpt' = 'do not accept'"));
        }
        
        // Check for response marker
        if (message.startsWith("^")) {
            suggestions.add(new ValidationSuggestion("^ indicates this is a response to a previous message"));
        }
        
        // Check for question blank
        if (message.contains("_")) {
            suggestions.add(new ValidationSuggestion("_ indicates what information you're asking for"));
        }
    }
    
    private static void validateCivsAndAgents(String message, List<ValidationIssue> issues, List<ValidationSuggestion> suggestions) {
        // Check for capital letter sequences (civ abbreviations)
        List<String> civs = CarniteParser.extractCivAbbreviations(message);
        
        if (!civs.isEmpty()) {
            suggestions.add(new ValidationSuggestion("Found civ abbreviations: " + String.join(", ", civs)));
        }
        
        // Check for ; without context
        if (message.equals(";")) {
            issues.add(new ValidationIssue(ValidationSeverity.WARNING, 
                "'; alone means 'my civ' - consider adding context"));
        }
    }
    
    private static void validateGrammar(String message, List<ValidationIssue> issues, List<ValidationSuggestion> suggestions) {
        // Carnite word order: Od Oi S V (Object-direct, Object-indirect, Subject, Verb)
        String[] words = message.split(" ");
        
        if (words.length >= 4 && !message.contains("_") && !message.startsWith("^")) {
            suggestions.add(new ValidationSuggestion(
                "Carnite word order: [What] [Where/To] [Who] [Action]"));
        }
        
        // Check for trade structure
        if (message.contains(";") && message.contains(":") && message.contains("_")) {
            suggestions.add(new ValidationSuggestion(
                "Trade format: '[offering]; _:' = 'My civ offers X, what will you give?'"));
        }
    }
    
    public static String getSyntaxHelp() {
        return """
            Carnite Telegraphic Quick Reference:
            
            Word Order: [What] [Where/To] [Who] [Action]
            Example: "dmd CN ; take" = "CN takes diamonds from my civ"
            
            Common Symbols:
            | = agent/player   : = your civ   ; = my civ
            , = property of    & = and        . = stack (64)
            _ = question       ^ = response   ~ = plural/about
            :: = all civs      - = not/negate
            
            Numbers: 2.5dmd = 2 stacks + 5 diamonds (133 total)
            Levels: bld|5 = level 5 builder
            """;
    }
    
    public enum ValidationSeverity {
        ERROR,
        WARNING,
        INFO
    }
    
    public record ValidationIssue(ValidationSeverity severity, String message) {}
    
    public record ValidationSuggestion(String suggestion) {}
    
    public record ValidationResult(
        boolean isValid,
        List<ValidationIssue> issues,
        List<ValidationSuggestion> suggestions
    ) {}
}
