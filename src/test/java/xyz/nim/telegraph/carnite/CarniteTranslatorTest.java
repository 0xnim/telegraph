package xyz.nim.telegraph.carnite;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import xyz.nim.telegraph.client.carnite.CarniteTranslator;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Carnite Translator")
class CarniteTranslatorTest {
    
    @Nested
    @DisplayName("Tense Colors")
    class TenseColorTests {
        
        @Test
        @DisplayName("White - Present statement")
        void testPresentStatement() {
            var result = CarniteTranslator.translate("frm NT CN bld", "white");
            assertEquals("Carnation is building a farm right now for Nautilus.", result.translation());
        }
        
        @Test
        @DisplayName("Light Gray - Past statement")
        void testPastStatement() {
            var result = CarniteTranslator.translate(".dmd CN ; trd", "light_gray");
            assertEquals("My civilization traded 1 stack (64) of diamonds to Carnation.", result.translation());
        }
        
        @Test
        @DisplayName("Dark Gray - Future statement")
        void testFutureStatement() {
            var result = CarniteTranslator.translate("frm NT CN bld", "dark_gray");
            assertEquals("Carnation will build a farm for Nautilus.", result.translation());
        }
        
        @Test
        @DisplayName("Pink - Conditional/Might")
        void testConditional() {
            var result = CarniteTranslator.translate("; mov", "pink");
            assertEquals("My civilization might move. Undecided.", result.translation());
        }
        
        @Test
        @DisplayName("Red - Urgent/High Priority")
        void testUrgent() {
            var result = CarniteTranslator.translate("~rd ;", "red");
            assertEquals("⚠ URGENT: Some raiders are at my civilization!", result.translation());
        }
        
        @Test
        @DisplayName("Light Blue - Request/Command/Suggestion")
        void testRequest() {
            var result = CarniteTranslator.translate("2bld CN:", "light_blue");
            assertEquals("My civilization should send 2 builders to Carnation.", result.translation());
        }
        
        @Test
        @DisplayName("Black - Opinion/Decision")
        void testDecision() {
            var result = CarniteTranslator.translate("dp| CN: ; snd", "black");
            assertEquals("It was decided that my civilization will send a diplomat to Carnation.", result.translation());
        }
        
        @Test
        @DisplayName("Blue - Yes/No Question")
        void testQuestion() {
            var result = CarniteTranslator.translate("CN _| atk", "blue");
            assertEquals("Who is attacking Carnation?", result.translation());
        }
        
        @Test
        @DisplayName("Yellow - Trade")
        void testTrade() {
            var result = CarniteTranslator.translate(".dmd ; _:", "yellow");
            assertEquals("My civilization offers 64 diamonds. What will you give in return?", result.translation());
        }
        
        @Test
        @DisplayName("Purple - Goal/Current Objective")
        void testGoal() {
            var result = CarniteTranslator.translate("acft ; get", "purple");
            assertEquals("My civilization's current goal is to get an autocrafter.", result.translation());
        }
    }
    
    @Nested
    @DisplayName("Quantity System")
    class QuantityTests {
        
        @Test
        @DisplayName("Singular item")
        void testSingular() {
            var result = CarniteTranslator.translate("blss,fd", "white");
            assertEquals("A piece of blessed food.", result.translation());
        }
        
        @Test
        @DisplayName("Plural with tilde")
        void testPlural() {
            var result = CarniteTranslator.translate("~blss,fd", "white");
            assertEquals("Some blessed food.", result.translation());
        }
        
        @Test
        @DisplayName("Specific quantity")
        void testSpecificQuantity() {
            var result = CarniteTranslator.translate("32irn", "white");
            assertEquals("32 iron.", result.translation());
        }
        
        @Test
        @DisplayName("One stack (64)")
        void testOneStack() {
            var result = CarniteTranslator.translate(".brd", "white");
            assertEquals("64 bread (1 stack).", result.translation());
        }
        
        @Test
        @DisplayName("Multiple stacks")
        void testMultipleStacks() {
            var result = CarniteTranslator.translate("2.dmd", "white");
            assertEquals("128 diamonds (2 stacks).", result.translation());
        }
        
        @Test
        @DisplayName("Stacks with remainder")
        void testStacksWithRemainder() {
            var result = CarniteTranslator.translate("3.32brd", "white");
            assertEquals("224 bread (3 stacks + 32).", result.translation());
        }
        
        @Test
        @DisplayName("Estimate with tilde")
        void testEstimate() {
            var result = CarniteTranslator.translate("~16blss,fd", "white");
            assertEquals("Around 16 blessed food.", result.translation());
        }
    }
    
    @Nested
    @DisplayName("Symbols and Special Markers")
    class SymbolTests {
        
        @Test
        @DisplayName("| - Individual agent")
        void testAgent() {
            var result = CarniteTranslator.translate("tdr|", "white");
            assertEquals("A trader.", result.translation());
        }
        
        @Test
        @DisplayName("; - My civilization")
        void testMyCiv() {
            var result = CarniteTranslator.translate(";", "white");
            assertEquals("My civilization.", result.translation());
        }
        
        @Test
        @DisplayName(": - Your civilization (speaking to)")
        void testYourCiv() {
            var result = CarniteTranslator.translate("CN:", "white");
            assertEquals("Carnation.", result.translation());
        }
        
        @Test
        @DisplayName("- - Negation")
        void testNegation() {
            var result = CarniteTranslator.translate("-atk", "white");
            assertEquals("Not attack.", result.translation());
        }
        
        @Test
        @DisplayName("^ - Response/In relation to")
        void testResponse() {
            var result = CarniteTranslator.translate("^ y", "white");
            assertEquals("In response: Yes.", result.translation());
        }
        
        @Test
        @DisplayName("_ - Content question")
        void testContentQuestion() {
            var result = CarniteTranslator.translate("_ CN atk", "blue");
            assertEquals("Who is attacking Carnation?", result.translation());
        }
        
        @Test
        @DisplayName("& - And")
        void testAnd() {
            var result = CarniteTranslator.translate("CN&EG", "white");
            assertEquals("Carnation and Eastguard.", result.translation());
        }
        
        @Test
        @DisplayName(":: - You all/anyone")
        void testYouAll() {
            var result = CarniteTranslator.translate("~:| :: gear", "light_blue");
            assertEquals("You all should gear up your citizens.", result.translation());
        }
    }
    
    @Nested
    @DisplayName("Word Order (Od Oi S V)")
    class WordOrderTests {
        
        @Test
        @DisplayName("Basic OOSV structure")
        void testBasicWordOrder() {
            var result = CarniteTranslator.translate("~rd SF DR atk", "white");
            assertEquals("Dwarven Republic is attacking some raiders at Sunfish.", result.translation());
        }
        
        @Test
        @DisplayName("With my civilization as subject")
        void testWithMyCiv() {
            var result = CarniteTranslator.translate("CN ; atk", "white");
            assertEquals("My civilization is attacking Carnation.", result.translation());
        }
        
        @Test
        @DisplayName("Implied 'is' statement")
        void testImpliedIs() {
            var result = CarniteTranslator.translate("lib|5 CM", "white");
            assertEquals("Cactus Mountain has a librarian level 5.", result.translation());
        }
    }
    
    @Nested
    @DisplayName("Questions")
    class QuestionTests {
        
        @Test
        @DisplayName("Who - person")
        void testWhoPerson() {
            var result = CarniteTranslator.translate("~dmd CV _| take", "blue");
            assertEquals("Who stole diamonds from Cannabis Village?", result.translation());
        }
        
        @Test
        @DisplayName("Who - civilization")
        void testWhoCiv() {
            var result = CarniteTranslator.translate("~dmd CV _ take", "blue");
            assertEquals("Which civ/What stole diamonds from Cannabis Village?", result.translation());
        }
        
        @Test
        @DisplayName("What - object")
        void testWhatObject() {
            var result = CarniteTranslator.translate("_ CV ~rd| take", "blue");
            assertEquals("What did raiders steal from Cannabis Village?", result.translation());
        }
        
        @Test
        @DisplayName("When")
        void testWhen() {
            var result = CarniteTranslator.translate("_t ~dmd CV ~rd| take", "blue");
            assertEquals("When did raiders steal diamonds from Cannabis Village?", result.translation());
        }
        
        @Test
        @DisplayName("Where")
        void testWhere() {
            var result = CarniteTranslator.translate("~dmd ~rd| _ take", "blue");
            assertEquals("Where did the raiders steal diamonds from?", result.translation());
        }
        
        @Test
        @DisplayName("Why")
        void testWhy() {
            var result = CarniteTranslator.translate("~dmd CV ~rd| take _rsn", "blue");
            assertEquals("Why did raiders steal diamonds from Cannabis Village?", result.translation());
        }
        
        @Test
        @DisplayName("How")
        void testHow() {
            var result = CarniteTranslator.translate("~dmd CV ~rd| take _", "blue");
            assertEquals("How did raiders steal diamonds from Cannabis Village?", result.translation());
        }
        
        @Test
        @DisplayName("How many")
        void testHowMany() {
            var result = CarniteTranslator.translate("_dmd CV ~rd| take", "blue");
            assertEquals("How many diamonds did raiders steal from Cannabis Village?", result.translation());
        }
    }
    
    @Nested
    @DisplayName("Trade Messages")
    class TradeTests {
        
        @Test
        @DisplayName("Simple trade offer")
        void testSimpleTradeOffer() {
            var result = CarniteTranslator.translate(".dmd ; _:", "yellow");
            assertEquals("My civilization offers 64 diamonds. What will you give in return?", result.translation());
            assertEquals("TRADE_OFFER", result.patternType());
        }
        
        @Test
        @DisplayName("Complex trade offer")
        void testComplexTradeOffer() {
            var result = CarniteTranslator.translate(".brd,32irn,.bndg ; _:", "yellow");
            assertEquals("My civilization offers 64 bread, 32 iron, and 64 bandages. What will you give in return?", result.translation());
        }
        
        @Test
        @DisplayName("Trade response")
        void testTradeResponse() {
            var result = CarniteTranslator.translate("2.gpdr,acft ; ^EG:", "yellow");
            assertEquals("My civilization offers 2 stacks of gunpowder and an autocrafter in response to Eastguard's previous trade offer.", result.translation());
        }
    }
    
    @Nested
    @DisplayName("Real-World Examples")
    class RealWorldTests {
        
        @Test
        @DisplayName("Raiders urgent alert")
        void testRaidersUrgent() {
            var result = CarniteTranslator.translate("~rd| ;", "red");
            assertEquals("⚠ URGENT: Some raiders are at my civilization!", result.translation());
        }
        
        @Test
        @DisplayName("Attack announcement")
        void testAttackAnnouncement() {
            var result = CarniteTranslator.translate("~NM,rd| SF DR; atk", "red");
            assertEquals("⚠ URGENT: Dwarven Republic is attacking the Nowy Madagaskar raiders at Sunfish!", result.translation());
        }
        
        @Test
        @DisplayName("Election announcement")
        void testElection() {
            var result = CarniteTranslator.translate("Elctn TWC", "white");
            assertEquals("There is going to be an election at the Twin Cities.", result.translation());
        }
        
        @Test
        @DisplayName("Player election result")
        void testPlayerElection() {
            var result = CarniteTranslator.translate("170| TWC; elct", "black");
            assertEquals("The Twin Cities decided to elect player 170.", result.translation());
        }
        
        @Test
        @DisplayName("Metagaming accusation")
        void testMetagaming() {
            var result = CarniteTranslator.translate("mtgm FTN", "blue");
            assertEquals("Is Fortun metagaming?", result.translation());
        }
        
        @Test
        @DisplayName("Surrender")
        void testSurrender() {
            var result = CarniteTranslator.translate("CRS: CV; srd", "white");
            assertEquals("My civilization, Cannabis Village, surrenders to The Crusaders.", result.translation());
        }
        
        @Test
        @DisplayName("Reject surrender")
        void testRejectSurrender() {
            var result = CarniteTranslator.translate("^ ; -acpt", "white");
            assertEquals("In response: We do not accept.", result.translation());
        }
    }
    
    @Nested
    @DisplayName("Pattern Detection")
    class PatternDetectionTests {
        
        @Test
        @DisplayName("Detect TRADE_OFFER pattern")
        void testPatternTrade() {
            var result = CarniteTranslator.translate(".dmd ; _:", "yellow");
            assertEquals("TRADE_OFFER", result.patternType());
        }
        
        @Test
        @DisplayName("Detect QUESTION pattern")
        void testPatternQuestion() {
            var result = CarniteTranslator.translate("_ CN atk", "blue");
            assertEquals("QUESTION", result.patternType());
        }
        
        @Test
        @DisplayName("Detect RESPONSE pattern")
        void testPatternResponse() {
            var result = CarniteTranslator.translate("^ y", "white");
            assertEquals("RESPONSE", result.patternType());
        }
        
        @Test
        @DisplayName("Detect STATEMENT pattern")
        void testPatternStatement() {
            var result = CarniteTranslator.translate("CN ; atk", "white");
            assertEquals("STATEMENT", result.patternType());
        }
    }
    
    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {
        
        @Test
        @DisplayName("Empty message")
        void testEmptyMessage() {
            var result = CarniteTranslator.translate("", "white");
            assertTrue(result.translation().isEmpty());
        }
        
        @Test
        @DisplayName("Null message")
        void testNullMessage() {
            var result = CarniteTranslator.translate(null, "white");
            assertTrue(result.translation().isEmpty());
        }
        
        @Test
        @DisplayName("Only symbols")
        void testOnlySymbols() {
            var result = CarniteTranslator.translate(";", "white");
            assertEquals("My civilization.", result.translation());
        }
        
        @Test
        @DisplayName("Multiple items with proper formatting")
        void testMultipleItemsFormatting() {
            var result = CarniteTranslator.translate(".brd,32irn,.bndg", "white");
            assertTrue(result.translation().contains("and"));
        }
        
        @Test
        @DisplayName("Components extraction")
        void testComponentsExtraction() {
            var result = CarniteTranslator.translate("2bld CN:", "white");
            assertFalse(result.components().isEmpty());
            assertTrue(result.components().contains("2bld"));
            assertTrue(result.components().contains("CN"));
        }
        
        @Test
        @DisplayName("CN :: ; atk with pink should be 'My civ might attack Carnation'")
        void testCNYouAllMyCivAttack() {
            var result = CarniteTranslator.translate("CN :: ; atk", "pink");
            // Pattern: CN is Od (direct object), :: addresses everyone, ; is S (my civ), atk is V
            // Expected: "My civilization might attack Carnation"
            String translation = result.translation();
            
            // Should have My civilization as subject
            assertTrue(translation.startsWith("My civilization"), 
                "Translation should start with 'My civilization', but was: " + translation);
            // Should have Carnation as object
            assertTrue(translation.contains("Carnation") || translation.contains("CN"), 
                "Translation should contain Carnation/CN as object, but was: " + translation);
        }
        
        @Test
        @DisplayName("CN :: ; atk with blue should be 'Is my civilization attacking Carnation?'")
        void testCNYouAllMyCivAttackBlue() {
            var result = CarniteTranslator.translate("CN :: ; atk", "blue");
            String translation = result.translation();
            System.out.println("Blue banner translation: " + translation);
            
            // Should be a yes/no question
            assertTrue(translation.startsWith("Is"), 
                "Translation should start with 'Is' for yes/no question, but was: " + translation);
            assertTrue(translation.contains("my civilization") || translation.contains("My civilization"), 
                "Translation should contain 'my civilization' as subject, but was: " + translation);
            assertTrue(translation.contains("Carnation") || translation.contains("CN"), 
                "Translation should contain Carnation/CN as object, but was: " + translation);
            assertTrue(translation.contains("attack"), 
                "Translation should contain 'attack', but was: " + translation);
        }
        
        @Test
        @DisplayName("NM,smth|5 die should parse civ property agent with level")
        void testCivPropertyAgentLevel() {
            // White - present
            var white = CarniteTranslator.translate("NM,smth|5 die", "white");
            System.out.println("White: " + white.translation());
            assertTrue(white.translation().contains("Nowy Madagaskar") || white.translation().contains("NM"),
                "Should contain civ name, but was: " + white.translation());
            assertTrue(white.translation().contains("blacksmith"),
                "Should contain 'blacksmith', but was: " + white.translation());
            assertTrue(white.translation().contains("5") || white.translation().contains("level 5"),
                "Should contain level 5, but was: " + white.translation());
            assertTrue(white.translation().toLowerCase().contains("dying") || white.translation().toLowerCase().contains("is dying"),
                "Should contain 'dying', but was: " + white.translation());
            
            // Light grey - past
            var past = CarniteTranslator.translate("NM,smth|5 die", "light_gray");
            System.out.println("Past: " + past.translation());
            assertTrue(past.translation().contains("died"),
                "Should contain 'died', but was: " + past.translation());
        }
    }
}
