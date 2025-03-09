package fix.parser;

import fix.parser.api.FixMessage;
import fix.parser.api.MissingTagException;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

class FixMessageImpl implements FixMessage {

    private static final byte SEPARATOR = 0x01;  // SOH
    private static final byte EQUALS = 0x3D;
    private final byte[] rawMessage;
    private final int[] tagPositions;  // Store positions of tags for quick lookup
    private final int[] valuePositions; // Store positions of values
    private final int[] valueLengths;   // Store lengths of values
    private final int tagCount;

    FixMessageImpl(byte[] message) {
        this.rawMessage = message;
        // Pre-calculate maximum possible number of fields (number of SOH characters)
        int maxFields = countOccurrences(message, SEPARATOR);
        tagPositions = new int[maxFields];
        valuePositions = new int[maxFields];
        valueLengths = new int[maxFields];
        tagCount = parseMessage();
    }

    private int parseMessage() {
        int fieldCount = 0;
        int pos = 0;

        while (pos < rawMessage.length) {
            // Store tag position
            tagPositions[fieldCount] = pos;

            // Find equals sign
            while (pos < rawMessage.length && rawMessage[pos] != EQUALS) {
                pos++;
            }
            pos++; // Skip equals sign

            // Store value position
            valuePositions[fieldCount] = pos;

            // Find SOH
            int valueStart = pos;
            while (pos < rawMessage.length && rawMessage[pos] != SEPARATOR) {
                pos++;
            }

            // Store value length
            valueLengths[fieldCount] = pos - valueStart;

            fieldCount++;
            pos++; // Skip SOH
        }

        return fieldCount;
    }

    private int findTag(int tag) {
        // Binary search could be used here if tags are sorted
        for (int i = 0; i < tagCount; i++) {
            int currentTag = parseTagAtIndex(i);
            if (currentTag == tag) {
                return i;
            }
        }
        return -1;
    }

    private int parseTagAtIndex(int index) {
        int end = valuePositions[index] - 1; // position of equals sign
        int result = 0;
        for (int i = tagPositions[index]; i < end; i++) {
            result = result * 10 + (rawMessage[i] - '0');
        }
        return result;
    }

    @Override
    public String getString(int tag) {
        int index = findTag(tag);
        if (index == -1) {
            throw new MissingTagException(tag);
        }
        return new String(rawMessage, valuePositions[index], valueLengths[index], StandardCharsets.ISO_8859_1);
    }

    @Override
    public int getInt(int tag) {
        return Integer.parseInt(getString(tag));
    }

    @Override
    public double getDouble(int tag) {
        return Double.parseDouble(getString(tag));
    }

    @Override
    public boolean hasTag(int tag) {
        return findTag(tag) != -1;
    }

    @Override
    public byte[] getRawValue(int tag) {
        int index = findTag(tag);
        if (index == -1) {
            throw new MissingTagException(tag);
        }
        return Arrays.copyOfRange(
                rawMessage,
                valuePositions[index],
                valuePositions[index] + valueLengths[index]
        );
    }

    private static int countOccurrences(byte[] array, byte target) {
        int count = 0;
        for (byte b : array) {
            if (b == target) {
                count++;
            }
        }
        return count;
    }
}