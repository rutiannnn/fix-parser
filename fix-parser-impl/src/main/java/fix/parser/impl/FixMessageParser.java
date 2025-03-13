package fix.parser.impl;

import fix.parser.message.base.FixMessage;
import fix.parser.message.base.Segment;
import fix.parser.message.base.UnderlyingMessage;
import fix.parser.messages44.Fields;
import fix.parser.spec.FixSpec;
import fix.parser.spec.FixType;
import fix.parser.spec.MessageDef;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class FixMessageParser {
    private static final byte FIELD_SEPARATOR = 0x01;  // SOH character
    private static final byte EQUALS_SIGN = 0x3D;

    private final FixSpec spec;
    private final String basePackage;
    private final Map<String, Class<? extends FixMessage>> messageTypeCache;

    public FixMessageParser(FixSpec spec, String basePackage) {
        this.spec = spec;
        this.basePackage = basePackage;
        this.messageTypeCache = new HashMap<>();
    }

    public FixMessage parse(byte[] messageBytes) {
        int fieldCount = countMaximumFields(messageBytes);

        int[] tags = new int[fieldCount];
        int[] valuePositions = new int[fieldCount];
        int[] valueLengths = new int[fieldCount];

        parseFields(messageBytes, tags, valuePositions, valueLengths);

        UnderlyingMessage underlyingMessage = new UnderlyingMessage(
            messageBytes,
            tags,
            valuePositions,
            valueLengths
        );

        Segment segment = new Segment(
            underlyingMessage,
            0,
            fieldCount,
            new Segment[]{}
        );

        String msgType = new String(
            messageBytes,
            valuePositions[findTagIndex(tags, Fields.MSGTYPE, 0, fieldCount)],
            valueLengths[findTagIndex(tags, Fields.MSGTYPE, 0, fieldCount)],
            StandardCharsets.ISO_8859_1
        );

        Class<? extends FixMessage> messageClass = getMessageClass(msgType);
        return createMessage(messageClass, segment);
    }

    private Class<? extends FixMessage> getMessageClass(String msgType) {
        Class<? extends FixMessage> messageClass = messageTypeCache.get(msgType);
        if (messageClass == null) {
            MessageDef messageDef = spec.messages().stream()
                .filter(m -> m.msgtype().equals(msgType))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Unknown message type: " + msgType));

            String className = basePackage + "." + messageDef.name() + "Message";
            try {
                messageClass = (Class<? extends FixMessage>) Class.forName(className);
                messageTypeCache.put(msgType, messageClass);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Failed to load message class: " + className, e);
            }
        }
        return messageClass;
    }

    private int countMaximumFields(byte[] messageBytes) {
        int count = 0;
        for (byte b : messageBytes) {
            if (b == FIELD_SEPARATOR) {
                count++;
            }
        }
        return count;
    }

    private void parseFields(byte[] messageBytes, int[] tags, int[] valuePositions, int[] valueLengths) {
        int fieldIndex = 0;
        int start = 0;

        while (start < messageBytes.length) {
            int equalsIndex = findEqualsSign(messageBytes, start, messageBytes.length);

            tags[fieldIndex] = Integer.parseInt(
                new String(messageBytes, start, equalsIndex - start, StandardCharsets.ISO_8859_1)
            );
            valuePositions[fieldIndex] = equalsIndex + 1;

            final int separatorIndex;
            if (spec.fieldsByNumber().get(tags[fieldIndex]).type() == FixType.DATA) {
                valueLengths[fieldIndex] = Integer.parseInt(
                    new String(messageBytes, valuePositions[fieldIndex - 1], valueLengths[fieldIndex - 1], StandardCharsets.ISO_8859_1)
                );
                separatorIndex = valuePositions[fieldIndex] + valueLengths[fieldIndex];
            } else {
                separatorIndex = findNextSeparator(messageBytes, start);
                valueLengths[fieldIndex] = separatorIndex - equalsIndex - 1;
            }

            fieldIndex++;
            start = separatorIndex + 1;
        }
    }

    private int findTagIndex(int[] tags, int targetTag, int start, int end) {
        for (int i = start; i < end; i++) {
            if (tags[i] == targetTag) {
                return i;
            }
        }
        return -1;
    }

    private int findNextSeparator(byte[] bytes, int start) {
        for (int i = start; i < bytes.length; i++) {
            if (bytes[i] == FIELD_SEPARATOR) {
                return i;
            }
        }
        return bytes.length;
    }

    private int findEqualsSign(byte[] bytes, int start, int end) {
        for (int i = start; i < end; i++) {
            if (bytes[i] == EQUALS_SIGN) {
                return i;
            }
        }
        return -1;
    }

    private FixMessage createMessage(Class<? extends FixMessage> messageClass, Segment segment) {
        try {
            return messageClass.getConstructor(Segment.class).newInstance(segment);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create message instance", e);
        }
    }
}
