package xyz.nim.telegraph.client.protocol.transport;

public record TransportEnvelope(
    String sourceId,
    String destinationId,
    String messageId,
    Integer partNumber,
    Integer totalParts,
    String addressee
) {
    public boolean isMultipart() {
        return partNumber != null && totalParts != null;
    }

    public boolean isBroadcast() {
        return "*".equals(destinationId);
    }

    public static TransportEnvelope empty() {
        return new TransportEnvelope(null, null, null, null, null, null);
    }

    public static TransportEnvelope single(String sourceId, String destinationId, String messageId) {
        return new TransportEnvelope(sourceId, destinationId, messageId, null, null, null);
    }

    public static TransportEnvelope multipart(String sourceId, String destinationId, String messageId, int partNumber, int totalParts) {
        return new TransportEnvelope(sourceId, destinationId, messageId, partNumber, totalParts, null);
    }

    public TransportEnvelope withAddressee(String addressee) {
        return new TransportEnvelope(sourceId, destinationId, messageId, partNumber, totalParts, addressee);
    }
}
