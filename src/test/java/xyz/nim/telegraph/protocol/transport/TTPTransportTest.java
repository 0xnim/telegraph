package xyz.nim.telegraph.protocol.transport;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import xyz.nim.telegraph.client.protocol.transport.DecodeResult;
import xyz.nim.telegraph.client.protocol.transport.TTPTransport;
import xyz.nim.telegraph.client.protocol.transport.TransportEnvelope;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TTP Transport Protocol Tests")
public class TTPTransportTest {

    private final TTPTransport ttp = new TTPTransport();

    @Test
    @DisplayName("Decode single-part message")
    void testDecodeSinglePart() {
        DecodeResult result = ttp.decode("37A BLUWOO:Gates open");

        assertFalse(result.isAck());
        assertFalse(result.isQuery());
        assertEquals("3", result.envelope().sourceId());
        assertEquals("7", result.envelope().destinationId());
        assertEquals("A", result.envelope().messageId());
        assertNull(result.envelope().partNumber());
        assertNull(result.envelope().totalParts());
        assertEquals("BLUWOO", result.envelope().addressee());
        assertEquals("Gates open", result.payload());
    }

    @Test
    @DisplayName("Decode single-part message without addressee")
    void testDecodeSinglePartNoAddressee() {
        DecodeResult result = ttp.decode("37A Simple message");

        assertEquals("3", result.envelope().sourceId());
        assertEquals("7", result.envelope().destinationId());
        assertEquals("A", result.envelope().messageId());
        assertNull(result.envelope().addressee());
        assertEquals("Simple message", result.payload());
    }

    @Test
    @DisplayName("Decode multi-part message")
    void testDecodeMultiPart() {
        DecodeResult result = ttp.decode("37B12 KINSTE:The northern border has been secur");

        assertEquals("3", result.envelope().sourceId());
        assertEquals("7", result.envelope().destinationId());
        assertEquals("B", result.envelope().messageId());
        assertEquals(1, result.envelope().partNumber());
        assertEquals(2, result.envelope().totalParts());
        assertTrue(result.envelope().isMultipart());
        assertEquals("KINSTE", result.envelope().addressee());
        assertEquals("The northern border has been secur", result.payload());
    }

    @Test
    @DisplayName("Decode multi-part continuation")
    void testDecodeMultiPartContinuation() {
        DecodeResult result = ttp.decode("37B22 ed. Patrols report no movement in valley.");

        assertEquals(2, result.envelope().partNumber());
        assertEquals(2, result.envelope().totalParts());
        assertNull(result.envelope().addressee());
        assertEquals("ed. Patrols report no movement in valley.", result.payload());
    }

    @Test
    @DisplayName("Decode ACK message with OK format")
    void testDecodeAck() {
        DecodeResult result = ttp.decode("73C OK B");

        assertTrue(result.isAck());
        assertEquals("7", result.envelope().sourceId());
        assertEquals("3", result.envelope().destinationId());
        assertEquals("C", result.envelope().messageId());
        assertEquals("B", result.payload());
    }

    @Test
    @DisplayName("Decode NEED message")
    void testDecodeNeed() {
        DecodeResult result = ttp.decode("73D NEED B3");

        assertTrue(result.isQuery());
        assertEquals("7", result.envelope().sourceId());
        assertEquals("3", result.envelope().destinationId());
        assertEquals("D", result.envelope().messageId());
        assertEquals("B3", result.payload());
    }

    @Test
    @DisplayName("Decode NEED message without part")
    void testDecodeNeedWithoutPart() {
        DecodeResult result = ttp.decode("73D NEED B");

        assertTrue(result.isQuery());
        assertEquals("B", result.payload());
    }

    @Test
    @DisplayName("Decode STATUS message")
    void testDecodeStatus() {
        DecodeResult result = ttp.decode("3*A STATUS OFFLINE 3 DAYS");

        assertTrue(result.isStatus());
        assertEquals("3", result.envelope().sourceId());
        assertEquals("*", result.envelope().destinationId());
        assertEquals("OFFLINE 3 DAYS", result.payload());
    }

    @Test
    @DisplayName("Decode STATUS ONLINE message")
    void testDecodeStatusOnline() {
        DecodeResult result = ttp.decode("3*B STATUS ONLINE");

        assertTrue(result.isStatus());
        assertEquals("ONLINE", result.payload());
    }

    @Test
    @DisplayName("Decode broadcast message")
    void testDecodeBroadcast() {
        DecodeResult result = ttp.decode("3*A Alert: Enemy spotted");

        assertEquals("3", result.envelope().sourceId());
        assertEquals("*", result.envelope().destinationId());
        assertTrue(result.envelope().isBroadcast());
        assertEquals("Alert: Enemy spotted", result.payload());
    }

    @Test
    @DisplayName("Pass through invalid format")
    void testPassthroughInvalidFormat() {
        DecodeResult result = ttp.decode("Not a TTP message");

        assertNull(result.envelope().sourceId());
        assertNull(result.envelope().destinationId());
        assertEquals("Not a TTP message", result.payload());
    }

    @Test
    @DisplayName("Encode single-part message")
    void testEncodeSinglePart() {
        TransportEnvelope envelope = TransportEnvelope.single("3", "7", "A")
            .withAddressee("BLUWOO");

        String encoded = ttp.encode("Gates open", envelope);

        assertEquals("37A BLUWOO:Gates open", encoded);
    }

    @Test
    @DisplayName("Encode multi-part message")
    void testEncodeMultiPart() {
        TransportEnvelope envelope = TransportEnvelope.multipart("3", "7", "B", 1, 2)
            .withAddressee("KINSTE");

        String encoded = ttp.encode("The northern border", envelope);

        assertEquals("37B12 KINSTE:The northern border", encoded);
    }

    @Test
    @DisplayName("Encode without envelope")
    void testEncodeWithoutEnvelope() {
        String encoded = ttp.encode("Raw message", null);
        assertEquals("Raw message", encoded);
    }

    @Test
    @DisplayName("Compute addressee hash")
    void testComputeAddressee() {
        assertEquals("IROGOL", TTPTransport.computeAddressee("Iron", "Golem"));
        assertEquals("REDBED", TTPTransport.computeAddressee("Red", "Bed"));
        assertEquals("JOXYUX", TTPTransport.computeAddressee("Jo", "Yu"));
        assertEquals("BLUWOO", TTPTransport.computeAddressee("Blue", "Wool"));
        assertEquals("STOBRI", TTPTransport.computeAddressee("Stone", "Brick"));
    }

    @Test
    @DisplayName("Protocol has envelope")
    void testHasEnvelope() {
        assertTrue(ttp.hasEnvelope());
    }

    @Test
    @DisplayName("Protocol name is TTP")
    void testProtocolName() {
        assertEquals("TTP", ttp.getName());
    }
}
