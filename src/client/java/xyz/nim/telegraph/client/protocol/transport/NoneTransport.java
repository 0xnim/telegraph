package xyz.nim.telegraph.client.protocol.transport;

public class NoneTransport implements TransportProtocol {
    public static final String NAME = "None";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "No transport protocol - raw messages";
    }

    @Override
    public DecodeResult decode(String rawMessage) {
        return DecodeResult.passthrough(rawMessage);
    }

    @Override
    public String encode(String payload, TransportEnvelope envelope) {
        return payload;
    }

    @Override
    public boolean hasEnvelope() {
        return false;
    }
}
