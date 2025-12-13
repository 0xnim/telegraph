package xyz.nim.telegraph.protocol.transport;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import xyz.nim.telegraph.client.protocol.transport.DecodeResult;
import xyz.nim.telegraph.client.protocol.transport.NoneTransport;
import xyz.nim.telegraph.client.protocol.transport.TransportEnvelope;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("None Transport Protocol Tests")
public class NoneTransportTest {

    private final NoneTransport none = new NoneTransport();

    @Test
    @DisplayName("Decode passes through message unchanged")
    void testDecodePassthrough() {
        DecodeResult result = none.decode("Any message content");

        assertEquals("Any message content", result.payload());
        assertNull(result.envelope().sourceId());
        assertNull(result.envelope().destinationId());
        assertFalse(result.isAck());
        assertFalse(result.isQuery());
    }

    @Test
    @DisplayName("Encode returns payload unchanged")
    void testEncodePassthrough() {
        TransportEnvelope envelope = TransportEnvelope.single("3", "7", "A");

        String encoded = none.encode("Message", envelope);

        assertEquals("Message", encoded);
    }

    @Test
    @DisplayName("Protocol has no envelope")
    void testHasNoEnvelope() {
        assertFalse(none.hasEnvelope());
    }

    @Test
    @DisplayName("Protocol name is None")
    void testProtocolName() {
        assertEquals("None", none.getName());
    }
}
