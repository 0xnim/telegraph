package xyz.nim.telegraph.client.protocol.transport;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TTPTransport implements TransportProtocol {
    public static final String NAME = "TTP";

    private static final Pattern SINGLE_HEADER = Pattern.compile("^([0-9A-F])([0-9A-F*])([0-9A-Z]) (.*)$");
    private static final Pattern MULTI_HEADER = Pattern.compile("^([0-9A-F])([0-9A-F*])([0-9A-Z])([1-9A-Z])([1-9A-Z]) (.*)$");
    private static final Pattern ADDRESSEE = Pattern.compile("^([A-Z]{6}):(.*)$");
    private static final Pattern ACK_PAYLOAD = Pattern.compile("^OK ([0-9A-Z])$");
    private static final Pattern NEED_PAYLOAD = Pattern.compile("^NEED ([0-9A-Z])([1-9A-Z])?$");
    private static final Pattern STATUS_PAYLOAD = Pattern.compile("^STATUS (.+)$");

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "Telegraph Transfer Protocol v1.0 - human-readable messaging";
    }

    @Override
    public DecodeResult decode(String rawMessage) {
        if (rawMessage == null || rawMessage.isEmpty()) {
            return DecodeResult.passthrough("");
        }

        Matcher multiMatcher = MULTI_HEADER.matcher(rawMessage);
        if (multiMatcher.matches()) {
            String source = multiMatcher.group(1);
            String dest = multiMatcher.group(2);
            String msgId = multiMatcher.group(3);
            int partNum = parseBase36(multiMatcher.group(4));
            int totalParts = parseBase36(multiMatcher.group(5));
            String payload = multiMatcher.group(6);

            TransportEnvelope envelope = TransportEnvelope.multipart(source, dest, msgId, partNum, totalParts);
            return parsePayload(envelope, payload);
        }

        Matcher singleMatcher = SINGLE_HEADER.matcher(rawMessage);
        if (singleMatcher.matches()) {
            String source = singleMatcher.group(1);
            String dest = singleMatcher.group(2);
            String msgId = singleMatcher.group(3);
            String payload = singleMatcher.group(4);

            TransportEnvelope envelope = TransportEnvelope.single(source, dest, msgId);
            return parsePayload(envelope, payload);
        }

        return DecodeResult.passthrough(rawMessage);
    }

    private DecodeResult parsePayload(TransportEnvelope envelope, String payload) {
        Matcher ackMatcher = ACK_PAYLOAD.matcher(payload);
        if (ackMatcher.matches()) {
            return DecodeResult.ack(envelope, ackMatcher.group(1));
        }

        Matcher needMatcher = NEED_PAYLOAD.matcher(payload);
        if (needMatcher.matches()) {
            String needInfo = needMatcher.group(1);
            if (needMatcher.group(2) != null) {
                needInfo += needMatcher.group(2);
            }
            return DecodeResult.query(envelope, needInfo);
        }

        Matcher statusMatcher = STATUS_PAYLOAD.matcher(payload);
        if (statusMatcher.matches()) {
            return DecodeResult.status(envelope, statusMatcher.group(1));
        }

        Matcher addresseeMatcher = ADDRESSEE.matcher(payload);
        if (addresseeMatcher.matches()) {
            String addressee = addresseeMatcher.group(1);
            String content = addresseeMatcher.group(2);
            return DecodeResult.standard(envelope.withAddressee(addressee), content);
        }

        return DecodeResult.standard(envelope, payload);
    }

    @Override
    public String encode(String payload, TransportEnvelope envelope) {
        if (envelope == null) {
            return payload;
        }

        StringBuilder sb = new StringBuilder();

        sb.append(envelope.sourceId() != null ? envelope.sourceId() : "0");
        sb.append(envelope.destinationId() != null ? envelope.destinationId() : "*");
        sb.append(envelope.messageId() != null ? envelope.messageId() : "0");

        if (envelope.isMultipart()) {
            sb.append(toBase36(envelope.partNumber()));
            sb.append(toBase36(envelope.totalParts()));
        }

        sb.append(" ");

        if (envelope.addressee() != null && !envelope.addressee().isEmpty()) {
            sb.append(envelope.addressee());
            sb.append(":");
        }

        sb.append(payload);

        return sb.toString();
    }

    @Override
    public boolean hasEnvelope() {
        return true;
    }

    private int parseBase36(String s) {
        if (s == null || s.isEmpty()) return 0;
        char c = s.charAt(0);
        if (c >= '0' && c <= '9') {
            return c - '0';
        } else if (c >= 'A' && c <= 'Z') {
            return 10 + (c - 'A');
        }
        return 0;
    }

    private String toBase36(int n) {
        if (n < 0) return "0";
        if (n <= 9) return String.valueOf((char) ('0' + n));
        if (n <= 35) return String.valueOf((char) ('A' + (n - 10)));
        return "Z";
    }

    public static String computeAddressee(String firstName, String lastName) {
        String first = padOrTrim(firstName.toUpperCase(), 3);
        String last = padOrTrim(lastName.toUpperCase(), 3);
        return first + last;
    }

    private static String padOrTrim(String s, int len) {
        if (s == null) s = "";
        s = s.replaceAll("[^A-Z]", "");
        if (s.length() >= len) {
            return s.substring(0, len);
        }
        while (s.length() < len) {
            s += "X";
        }
        return s;
    }
}
