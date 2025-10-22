package xyz.nim.telegraph.carnite;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import xyz.nim.telegraph.client.carnite.CarniteExplainer;
import xyz.nim.telegraph.client.carnite.CarniteParser;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Carnite Explainer Tests")
public class CarniteExplainerTest {
    
    @Test
    @DisplayName("Beginner: Simple raider message")
    void testSimpleRaiders() {
        var result = CarniteExplainer.explainMessage("~rd| ;", "red");
        assertNotNull(result);
        assertTrue(result.translation().toLowerCase().contains("raid") || 
                   result.translation().toLowerCase().contains("my"));
        assertTrue(result.parts().size() > 0);
    }
    
    @Test
    @DisplayName("Beginner: Builder to civ")
    void testBuildersToGiv() {
        var result = CarniteExplainer.explainMessage("2bld| CN:", "light_blue");
        assertNotNull(result);
        assertTrue(result.translation().contains("builder"));
        assertTrue(result.translation().contains("CN"));
    }
    
    @Test
    @DisplayName("Beginner: Diamond trade")
    void testDiamondTrade() {
        var result = CarniteExplainer.explainMessage(".dmd CN ; trd", "light_gray");
        assertNotNull(result);
        assertTrue(result.translation().toLowerCase().contains("diamond") || 
                   result.translation().toLowerCase().contains("cn"));
        assertTrue(result.parts().size() > 0);
    }
    
    @Test
    @DisplayName("Intermediate: Trade offer with stacks")
    void testTradeOfferWithStacks() {
        var result = CarniteExplainer.explainMessage("2.5dmd,32irn ; _:", "yellow");
        assertNotNull(result);
        String translation = result.translation().toLowerCase();
        assertTrue(translation.contains("offer") || translation.contains("give"), 
                   "Translation should mention offer or give. Got: " + result.translation());
        assertTrue(translation.contains("diamond") && translation.contains("iron"),
                   "Translation should mention both diamonds and iron. Got: " + result.translation());
        assertTrue(result.parts().size() > 5);
    }
    
    @Test
    @DisplayName("Intermediate: Who is attacking question")
    void testWhoIsAttacking() {
        var result = CarniteExplainer.explainMessage("_ CN atk", "blue");
        assertNotNull(result);
        assertTrue(result.translation().toLowerCase().contains("who"));
        assertTrue(result.translation().contains("CN"));
    }
    
    @Test
    @DisplayName("Intermediate: Negative response")
    void testNegativeResponse() {
        var result = CarniteExplainer.explainMessage("^ -acpt ; CN:", "white");
        assertNotNull(result);
        assertTrue(result.translation().toLowerCase().contains("response") || 
                   result.translation().contains("not accept"));
    }
    
    @Test
    @DisplayName("Intermediate: Call for builders")
    void testCallForBuilders() {
        var result = CarniteExplainer.explainMessage("~5bld|3 SF: ; call", "red");
        assertNotNull(result);
        assertTrue(result.parts().size() > 0);
    }
    
    @Test
    @DisplayName("Advanced: Multiple entities trade")
    void testMultipleEntitiesTrade() {
        var result = CarniteExplainer.explainMessage("2bld|5&3mn|4 CN: EG; trd", "light_gray");
        assertNotNull(result);
        assertTrue(result.translation().contains("builder") || result.translation().contains("miner"));
    }
    
    @Test
    @DisplayName("Advanced: Trade to multiple civs")
    void testTradeToMultipleCivs() {
        var result = CarniteExplainer.explainMessage(".brd,32irn,.bndg ; _::", "yellow");
        assertNotNull(result);
        assertTrue(result.translation().contains("bread") || 
                   result.translation().contains("iron") || 
                   result.translation().contains("bandage"));
    }
    
    @Test
    @DisplayName("Advanced: Attack raiders at location")
    void testAttackRaidersAtLocation() {
        var result = CarniteExplainer.explainMessage("~NM,rd| SF DR; atk", "red");
        assertNotNull(result);
        var civs = CarniteParser.extractCivAbbreviations("~NM,rd| SF DR; atk");
        assertTrue(civs.size() >= 2);
    }
    
    @Test
    @DisplayName("Advanced: Quote with decision")
    void testQuoteWithDecision() {
        var result = CarniteExplainer.explainMessage("'acpt' CN: ; -acpt", "black");
        assertNotNull(result);
        assertTrue(result.parts().stream().anyMatch(p -> 
            p.type() == CarniteExplainer.MessagePartType.QUOTE));
    }
    
    @Test
    @DisplayName("Expert: Time threat")
    void testTimeThreat() {
        var result = CarniteExplainer.explainMessage("2h -~rd|; 32dmd: SF:", "red");
        assertNotNull(result);
        assertTrue(result.translation().contains("2") || result.translation().contains("hour"));
    }
    
    @Test
    @DisplayName("Expert: Enchant trade offer")
    void testEnchantTradeOffer() {
        var result = CarniteExplainer.explainMessage("~ench; _: PH:", "yellow");
        assertNotNull(result);
        assertTrue(CarniteParser.isTradeMessage("~ench; _: PH:"));
    }
    
    @Test
    @DisplayName("Expert: Response with attack")
    void testResponseWithAttack() {
        var result = CarniteExplainer.explainMessage("IT: ^ atk CN", "white");
        assertNotNull(result);
        var civs = CarniteParser.extractCivAbbreviations("IT: ^ atk CN");
        assertTrue(civs.contains("IT") && civs.contains("CN"));
    }
    
    @Test
    @DisplayName("Expert: Meeting call to all")
    void testMeetingCallToAll() {
        var result = CarniteExplainer.explainMessage("metng :: ; call", "light_blue");
        assertNotNull(result);
        assertTrue(result.parts().size() > 0);
    }
    
    @Test
    @DisplayName("Expert: Secret status")
    void testSecretStatus() {
        var result = CarniteExplainer.explainMessage("scrt,-die CN;", "pink");
        assertNotNull(result);
        assertTrue(result.parts().stream().anyMatch(p -> 
            p.type() == CarniteExplainer.MessagePartType.MODIFIER));
    }
    
    @Test
    @DisplayName("Expert: Election result")
    void testElectionResult() {
        var result = CarniteExplainer.explainMessage("170| TWC; elct", "light_gray");
        assertNotNull(result);
        assertTrue(result.parts().stream().anyMatch(p -> 
            p.type() == CarniteExplainer.MessagePartType.ENTITY));
    }
    
    @Test
    @DisplayName("Nightmare: Complex multi-part trade")
    void testComplexMultiPartTrade() {
        var result = CarniteExplainer.explainMessage("2.dmd,~64irn,3.brd CN: EG&DR; trd ^ SF,rd|: atk", "light_gray");
        assertNotNull(result);
        assertTrue(result.parts().size() > 10);
    }
    
    @Test
    @DisplayName("Nightmare: Complex question")
    void testComplexQuestion() {
        var result = CarniteExplainer.explainMessage("_ CN ; bld|&mn|&lib|5 total", "blue");
        assertNotNull(result);
        assertTrue(result.parts().stream().anyMatch(p -> 
            p.type() == CarniteExplainer.MessagePartType.QUESTION));
    }
    
    @Test
    @DisplayName("Nightmare: Crusader warning")
    void testCrusaderWarning() {
        var result = CarniteExplainer.explainMessage("~CSD| 5m PH:", "red");
        assertNotNull(result);
        var civs = CarniteParser.extractCivAbbreviations("~CSD| 5m PH:");
        assertTrue(civs.size() >= 2);
    }
    
    @Test
    @DisplayName("Nightmare: Complex response chain")
    void testComplexResponseChain() {
        var result = CarniteExplainer.explainMessage("-die CN _ ; ^ SF,dp|: -acpt trd", "black");
        assertNotNull(result);
        assertTrue(result.parts().stream().anyMatch(p -> 
            p.type() == CarniteExplainer.MessagePartType.RESPONSE));
    }
    
    @Test
    @DisplayName("Nightmare: Counter-trade")
    void testCounterTrade() {
        var result = CarniteExplainer.explainMessage("2.5dmd,32irn ; _: ^ NM: .gpdr,acft", "yellow");
        assertNotNull(result);
        assertTrue(CarniteParser.isTradeMessage("2.5dmd,32irn ; _: ^ NM: .gpdr,acft"));
    }
    
    @Test
    @DisplayName("Real-world: Panic message")
    void testPanicMessage() {
        var result = CarniteExplainer.explainMessage("~rd| ;", "red");
        assertNotNull(result);
        assertEquals("Urgent/High Priority", CarniteParser.getTenseFromColor("red"));
    }
    
    @Test
    @DisplayName("Real-world: Raid invitation")
    void testRaidInvitation() {
        var result = CarniteExplainer.explainMessage("SF&SH NM:&CN; rd", "light_blue");
        assertNotNull(result);
        var civs = CarniteParser.extractCivAbbreviations("SF&SH NM:&CN; rd");
        assertTrue(civs.size() >= 3);
    }
    
    @Test
    @DisplayName("Real-world: Wanted alert")
    void testWantedAlert() {
        var result = CarniteExplainer.explainMessage("rd&arsn 990| wtd", "red");
        assertNotNull(result);
        assertTrue(result.parts().stream().anyMatch(p -> 
            p.type() == CarniteExplainer.MessagePartType.ENTITY));
    }
    
    @Test
    @DisplayName("Real-world: Surrender")
    void testSurrender() {
        var result = CarniteExplainer.explainMessage("CRS: CV; srd", "white");
        assertNotNull(result);
        var civs = CarniteParser.extractCivAbbreviations("CRS: CV; srd");
        assertEquals(2, civs.size());
    }
    
    @Test
    @DisplayName("Real-world: Reject surrender")
    void testRejectSurrender() {
        var result = CarniteExplainer.explainMessage("^ ; -acpt", "black");
        assertNotNull(result);
        assertTrue(result.parts().stream().anyMatch(p -> 
            p.type() == CarniteExplainer.MessagePartType.MODIFIER));
    }
    
    @Test
    @DisplayName("Edge case: Double my-civ")
    void testDoubleMyCiv() {
        var result = CarniteExplainer.explainMessage(";;", "white");
        assertNotNull(result);
        // Should not crash
    }
    
    @Test
    @DisplayName("Edge case: Multiple agents")
    void testMultipleAgents() {
        var result = CarniteExplainer.explainMessage("|||||", "white");
        assertNotNull(result);
        // Should not crash
    }
    
    @Test
    @DisplayName("Edge case: Multiple questions")
    void testMultipleQuestions() {
        var result = CarniteExplainer.explainMessage("_____", "blue");
        assertNotNull(result);
        assertTrue(result.parts().stream().anyMatch(p -> 
            p.type() == CarniteExplainer.MessagePartType.QUESTION));
    }
    
    @Test
    @DisplayName("Edge case: Chained civs")
    void testChainedCivs() {
        var result = CarniteExplainer.explainMessage("CN:CN:CN:", "blue");
        assertNotNull(result);
        var civs = CarniteParser.extractCivAbbreviations("CN:CN:CN:");
        assertTrue(civs.size() >= 1);
    }
    
    @Test
    @DisplayName("Edge case: Multiple plurals")
    void testMultiplePlurals() {
        var result = CarniteExplainer.explainMessage("~~~~~rd|", "red");
        assertNotNull(result);
        // Should not crash
    }
    
    @Test
    @DisplayName("Edge case: Multiple negations")
    void testMultipleNegations() {
        var result = CarniteExplainer.explainMessage("-atk -rd -trd", "black");
        assertNotNull(result);
        assertTrue(result.parts().stream().anyMatch(p -> 
            p.type() == CarniteExplainer.MessagePartType.MODIFIER));
    }
}
