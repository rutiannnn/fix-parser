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
    private final int startPosition; // Start position in rawMessage for group handling
    private final int endPosition;   // End position in rawMessage for group handling

    FixMessageImpl(byte[] message) {
        this(message, 0, message.length);
    }

    // Constructor for repeating groups - uses a subset of the original message
    private FixMessageImpl(byte[] message, int start, int end) {
        this.rawMessage = message;
        this.startPosition = start;
        this.endPosition = end;

        // Pre-calculate maximum possible number of fields
        int maxFields = countOccurrences(message, SEPARATOR, start, end);
        tagPositions = new int[maxFields];
        valuePositions = new int[maxFields];
        valueLengths = new int[maxFields];
        tagCount = parseMessage();
    }

    private int parseMessage() {
        int fieldCount = 0;
        int pos = startPosition;

        while (pos < endPosition) {
            // Store tag position
            tagPositions[fieldCount] = pos;

            // Find equals sign
            while (pos < endPosition && rawMessage[pos] != EQUALS) {
                pos++;
            }
            pos++; // Skip equals sign

            // Store value position
            valuePositions[fieldCount] = pos;

            // Find SOH
            int valueStart = pos;
            while (pos < endPosition && rawMessage[pos] != SEPARATOR) {
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
            int currentTag = parseTag(rawMessage, tagPositions[i], valuePositions[i] - 1);
            if (currentTag == tag) {
                return i;
            }
        }
        return -1;
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

    @Override
    public FixMessage[] getGroups(int groupCountTag, int[] groupTags) {
        // Find the group count field
        int countIndex = findTag(groupCountTag);
        if (countIndex == -1) {
            throw new MissingTagException(groupCountTag);
        }

        // Get number of entries in the group
        int numGroups = getInt(groupCountTag);
        if (numGroups == 0) {
            return new FixMessage[0];
        }

        FixMessage[] groups = new FixMessage[numGroups];

        // Start position is the tag after the group count tag
        int currentPos = tagPositions[countIndex + 1];
        int firstTag = parseTag(rawMessage, currentPos, valuePositions[countIndex + 1] - 1);

        // Parse each group entry
        for (int i = 0; i < numGroups; i++) {
            // Find the end of this group entry
            int groupEnd = findGroupEnd(currentPos, firstTag, groupTags);

            // Create a new FixMessage for this group entry
            groups[i] = new FixMessageImpl(rawMessage, currentPos, groupEnd);

            // Move to the start of the next group
            currentPos = groupEnd;
        }

        return groups;
    }

    private int findGroupEnd(int startPos, int firstTag, int[] groupTags) {
        int pos = startPos;
        boolean foundFirstTag = false;
        while (pos < endPosition) {
            // Store current position
            int fieldStart = pos;

            // Find equals sign
            while (pos < endPosition && rawMessage[pos] != EQUALS) {
                pos++;
            }

            // Parse the tag at this position
            int currentTag = parseTag(rawMessage, fieldStart, pos);

            // If we find the first tag again, this is the end of the current group
            if (currentTag == firstTag) {
                if (foundFirstTag) {
                    return fieldStart;
                } else {
                    foundFirstTag = true;
                }
            }

            // If the current tag is not in the allowed tags list, this is the end of the group
            if (!isTagInGroup(currentTag, groupTags)) {
                return fieldStart;
            }

            // Skip value and SOH
            while (pos < endPosition && rawMessage[pos] != SEPARATOR) {
                pos++;
            }
            pos++; // Skip SOH
        }

        return endPosition;
    }

    private boolean isTagInGroup(int tag, int[] groupTags) {
        for (int groupTag : groupTags) {
            if (tag == groupTag) {
                return true;
            }
        }
        return false;
    }

    private static int parseTag(byte[] message, int start, int end) {
        int tag = 0;
        for (int i = start; i < end; i++) {
            tag = tag * 10 + (message[i] - '0');
        }
        return tag;
    }

    private static int countOccurrences(byte[] array, byte target, int start, int end) {
        int count = 0;
        for (int i = start; i < end; i++) {
            if (array[i] == target) {
                count++;
            }
        }
        return count;
    }
}