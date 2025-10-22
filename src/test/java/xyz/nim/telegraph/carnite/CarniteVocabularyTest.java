package xyz.nim.telegraph.carnite;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import xyz.nim.telegraph.client.carnite.CarniteVocabulary;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Carnite Vocabulary Tests")
public class CarniteVocabularyTest {
    
    @Test
    @DisplayName("Expand common abbreviation - raid")
    void testExpandRaid() {
        assertEquals("raid", CarniteVocabulary.expand("rd"));
    }
    
    @Test
    @DisplayName("Expand common abbreviation - attack")
    void testExpandAttack() {
        assertEquals("attack", CarniteVocabulary.expand("atk"));
    }
    
    @Test
    @DisplayName("Expand common abbreviation - builder")
    void testExpandBuilder() {
        assertEquals("builder", CarniteVocabulary.expand("bld"));
    }
    
    @Test
    @DisplayName("Expand common abbreviation - diamond")
    void testExpandDiamond() {
        assertEquals("diamond", CarniteVocabulary.expand("dmd"));
    }
    
    @Test
    @DisplayName("Expand common abbreviation - iron")
    void testExpandIron() {
        assertEquals("iron", CarniteVocabulary.expand("irn"));
    }
    
    @Test
    @DisplayName("Expand unknown word returns same")
    void testExpandUnknown() {
        assertEquals("xyz", CarniteVocabulary.expand("xyz"));
    }
    
    @Test
    @DisplayName("Abbreviate known word - raid")
    void testAbbreviateRaid() {
        assertEquals("rd", CarniteVocabulary.abbreviate("raid"));
    }
    
    @Test
    @DisplayName("Abbreviate known word - attack")
    void testAbbreviateAttack() {
        assertEquals("atk", CarniteVocabulary.abbreviate("attack"));
    }
    
    @Test
    @DisplayName("Get autocomplete suggestions")
    void testAutocompleteSuggestions() {
        List<String> suggestions = CarniteVocabulary.getAutocompleteSuggestions("rd");
        assertFalse(suggestions.isEmpty());
        assertTrue(suggestions.stream().anyMatch(s -> s.contains("raid")));
    }
    
    @Test
    @DisplayName("Get autocomplete for 'at'")
    void testAutocompleteAt() {
        List<String> suggestions = CarniteVocabulary.getAutocompleteSuggestions("at");
        assertTrue(suggestions.stream().anyMatch(s -> s.contains("attack")));
    }
    
    @Test
    @DisplayName("Get all abbreviations")
    void testGetAllAbbreviations() {
        var abbrs = CarniteVocabulary.getAllAbbreviations();
        assertFalse(abbrs.isEmpty());
        assertTrue(abbrs.containsKey("rd"));
        assertTrue(abbrs.containsKey("atk"));
        assertTrue(abbrs.containsKey("bld"));
    }
    
    @Test
    @DisplayName("Get all symbols")
    void testGetAllSymbols() {
        var symbols = CarniteVocabulary.getSymbols();
        assertFalse(symbols.isEmpty());
        assertTrue(symbols.containsKey("|"));
        assertTrue(symbols.containsKey(":"));
        assertTrue(symbols.containsKey(";"));
    }
    
    @Test
    @DisplayName("Get banner colors")
    void testGetBannerColors() {
        var colors = CarniteVocabulary.getBannerColors();
        assertFalse(colors.isEmpty());
        assertTrue(colors.stream().anyMatch(c -> c.contains("White")));
        assertTrue(colors.stream().anyMatch(c -> c.contains("Red")));
        assertTrue(colors.stream().anyMatch(c -> c.contains("Yellow")));
    }
    
    @Test
    @DisplayName("Format message with expansion")
    void testFormatWithExpansion() {
        String formatted = CarniteVocabulary.formatWithExpansion("rd atk");
        assertTrue(formatted.contains("raid"));
        assertTrue(formatted.contains("attack"));
    }
}
