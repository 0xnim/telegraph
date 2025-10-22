package xyz.nim.telegraph.carnite;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import xyz.nim.telegraph.client.carnite.CarniteValidator;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Carnite Validator Tests")
public class CarniteValidatorTest {
    
    @Test
    @DisplayName("Validate empty message")
    void testValidateEmpty() {
        var result = CarniteValidator.validate("", "white");
        assertFalse(result.isValid());
        assertTrue(result.issues().stream().anyMatch(i -> 
            i.severity() == CarniteValidator.ValidationSeverity.ERROR));
    }
    
    @Test
    @DisplayName("Validate short message")
    void testValidateShort() {
        var result = CarniteValidator.validate("~rd| ;", "red");
        assertTrue(result.isValid());
    }
    
    @Test
    @DisplayName("Validate message over 32 chars")
    void testValidateOver32Chars() {
        var result = CarniteValidator.validate("this is a very long message that exceeds thirty two characters", "white");
        assertFalse(result.issues().isEmpty());
        assertTrue(result.issues().stream().anyMatch(i -> 
            i.severity() == CarniteValidator.ValidationSeverity.WARNING));
    }
    
    @Test
    @DisplayName("Validate message over 38 chars")
    void testValidateOver38Chars() {
        var result = CarniteValidator.validate("this is an extremely long message that exceeds thirty eight characters for sure", "white");
        assertFalse(result.issues().isEmpty());
        assertTrue(result.issues().stream().anyMatch(i -> 
            i.severity() == CarniteValidator.ValidationSeverity.WARNING));
    }
    
    @Test
    @DisplayName("Validate trade with wrong banner color")
    void testValidateTradeWrongColor() {
        var result = CarniteValidator.validate(".dmd ; _:", "red");
        assertFalse(result.issues().isEmpty());
        assertTrue(result.issues().stream().anyMatch(i -> 
            i.message().toLowerCase().contains("yellow")));
    }
    
    @Test
    @DisplayName("Validate trade with correct banner color")
    void testValidateTradeCorrectColor() {
        var result = CarniteValidator.validate(".dmd ; _:", "yellow");
        assertTrue(result.isValid());
    }
    
    @Test
    @DisplayName("Validate provides suggestions")
    void testValidateProvidesSuggestions() {
        var result = CarniteValidator.validate("CN: ; atk _", "white");
        assertFalse(result.suggestions().isEmpty());
    }
    
    @Test
    @DisplayName("Validate message with negation")
    void testValidateNegation() {
        var result = CarniteValidator.validate("-acpt CN:", "black");
        assertTrue(result.suggestions().stream().anyMatch(s -> 
            s.suggestion().contains("-")));
    }
    
    @Test
    @DisplayName("Validate message with response")
    void testValidateResponse() {
        var result = CarniteValidator.validate("^ y", "white");
        assertTrue(result.suggestions().stream().anyMatch(s -> 
            s.suggestion().contains("^")));
    }
    
    @Test
    @DisplayName("Validate message with question blank")
    void testValidateQuestionBlank() {
        var result = CarniteValidator.validate("_ CN atk", "blue");
        assertTrue(result.suggestions().stream().anyMatch(s -> 
            s.suggestion().contains("_")));
    }
}
