package xyz.nim.telegraph.client.protocol.transport;

public interface TransportProtocol {
    String getName();

    String getDescription();

    DecodeResult decode(String rawMessage);

    String encode(String payload, TransportEnvelope envelope);

    boolean hasEnvelope();
}
