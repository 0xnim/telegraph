package xyz.nim.telegraph.client.protocol.transport;

public record DecodeResult(
    TransportEnvelope envelope,
    String payload,
    MessageType messageType
) {
    public enum MessageType {
        STANDARD,
        ACK,
        QUERY,
        STATUS
    }

    public static DecodeResult standard(TransportEnvelope envelope, String payload) {
        return new DecodeResult(envelope, payload, MessageType.STANDARD);
    }

    public static DecodeResult ack(TransportEnvelope envelope, String ackedMessageId) {
        return new DecodeResult(envelope, ackedMessageId, MessageType.ACK);
    }

    public static DecodeResult query(TransportEnvelope envelope, String queryInfo) {
        return new DecodeResult(envelope, queryInfo, MessageType.QUERY);
    }

    public static DecodeResult status(TransportEnvelope envelope, String statusInfo) {
        return new DecodeResult(envelope, statusInfo, MessageType.STATUS);
    }

    public static DecodeResult passthrough(String payload) {
        return new DecodeResult(TransportEnvelope.empty(), payload, MessageType.STANDARD);
    }

    public boolean isAck() {
        return messageType == MessageType.ACK;
    }

    public boolean isQuery() {
        return messageType == MessageType.QUERY;
    }

    public boolean isStatus() {
        return messageType == MessageType.STATUS;
    }
}
