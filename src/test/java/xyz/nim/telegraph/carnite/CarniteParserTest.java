package xyz.nim.telegraph.carnite;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import xyz.nim.telegraph.client.carnite.CarniteParser;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Carnite Parser Tests")
public class CarniteParserTest {
    
    @Test
    @DisplayName("Extract civilizations from simple message")
    void testExtractCivilizationsSimple() {
        List<String> civs = CarniteParser.extractCivAbbreviations("CN: ; atk");
        assertTrue(civs.contains("CN"));
        assertEquals(1, civs.size());
    }
    
    @Test
    @DisplayName("Extract multiple civilizations")
    void testExtractMultipleCivilizations() {
        List<String> civs = CarniteParser.extractCivAbbreviations("CN: EG; trd DR");
        assertTrue(civs.contains("CN"));
        assertTrue(civs.contains("EG"));
        assertTrue(civs.contains("DR"));
        assertEquals(3, civs.size());
    }
    
    @Test
    @DisplayName("Detect trade message with semicolon and colon")
    void testDetectTradeMessage() {
        assertTrue(CarniteParser.isTradeMessage(".dmd ; _:") || 
                   CarniteParser.isTradeMessage(".dmd ;_:"));
        assertTrue(CarniteParser.isTradeMessage("2.5dmd,32irn ; _:"));
        // Message with both ; and : is a trade
        assertTrue(CarniteParser.isTradeMessage("test ; test:"));
    }
    
    @Test
    @DisplayName("Parse trade offer")
    void testParseTradeOffer() {
        var trade = CarniteParser.parseTradeOffer(".brd,32irn ; _:");
        assertNotNull(trade);
        assertTrue(trade.offering().contains("brd") || trade.offering().contains("irn"));
    }
    
    @Test
    @DisplayName("Get tense from white banner")
    void testGetTenseWhite() {
        assertEquals("Present Statement", CarniteParser.getTenseFromColor("white"));
    }
    
    @Test
    @DisplayName("Get tense from red banner")
    void testGetTenseRed() {
        assertEquals("Urgent/High Priority", CarniteParser.getTenseFromColor("red"));
    }
    
    @Test
    @DisplayName("Get tense from yellow banner")
    void testGetTenseYellow() {
        assertEquals("Trade Offer", CarniteParser.getTenseFromColor("yellow"));
    }
    
    @Test
    @DisplayName("Get tense from blue banner")
    void testGetTenseBlue() {
        assertEquals("Y/N Question", CarniteParser.getTenseFromColor("blue"));
    }
    
    @Test
    @DisplayName("Parse message with all symbol types")
    void testParseAllSymbols() {
        var result = CarniteParser.parse("|:;,&._^'~-", "white");
        assertNotNull(result);
        assertTrue(result.tokens().size() > 5);
    }
    
    @Test
    @DisplayName("Tokenize simple message")
    void testTokenizeSimple() {
        var result = CarniteParser.parse("2bld| CN:", "white");
        assertNotNull(result);
        assertTrue(result.tokens().stream().anyMatch(t -> 
            t.type() == CarniteParser.CarniteTokenType.AGENT));
        assertTrue(result.tokens().stream().anyMatch(t -> 
            t.type() == CarniteParser.CarniteTokenType.YOUR_CIV));
    }
    
    @Test
    @DisplayName("Tokenize trade message")
    void testTokenizeTrade() {
        var result = CarniteParser.parse(".dmd ; _:", "yellow");
        assertNotNull(result);
        assertTrue(result.tokens().stream().anyMatch(t -> 
            t.type() == CarniteParser.CarniteTokenType.MY_CIV));
        assertTrue(result.tokens().stream().anyMatch(t -> 
            t.type() == CarniteParser.CarniteTokenType.QUESTION_BLANK));
        assertTrue(result.tokens().stream().anyMatch(t -> 
            t.type() == CarniteParser.CarniteTokenType.YOUR_CIV));
    }
    
    @Test
    @DisplayName("Tokenize quoted text")
    void testTokenizeQuoted() {
        var result = CarniteParser.parse("'acpt' CN:", "black");
        assertNotNull(result);
        assertTrue(result.tokens().stream().anyMatch(t -> 
            t.type() == CarniteParser.CarniteTokenType.QUOTED));
    }
    
    @Test
    @DisplayName("Extract no civs from lowercase")
    void testExtractNoCivsFromLowercase() {
        List<String> civs = CarniteParser.extractCivAbbreviations("bld mn atk");
        assertEquals(0, civs.size());
    }
    
    @Test
    @DisplayName("Extract civs with varying lengths")
    void testExtractCivsVaryingLengths() {
        List<String> civs = CarniteParser.extractCivAbbreviations("CN DR TWC NMDG");
        assertTrue(civs.contains("CN"));
        assertTrue(civs.contains("DR"));
        assertTrue(civs.contains("TWC"));
        assertTrue(civs.contains("NMDG"));
    }
}
