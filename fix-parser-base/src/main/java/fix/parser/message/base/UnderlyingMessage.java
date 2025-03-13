package fix.parser.message.base;

/**
 * Represents the underlying FIX message with its raw data and index arrays for efficient field access.
 */
public record UnderlyingMessage(
    byte[] rawMessage,     // Raw message bytes
    int[] tags,
    // tag[i] is the tag number of the i-th field (including nested groups/components) in the rawMessage
    int[] valuePositions,
    // rawMessage[valuePositions[i]] is the first byte of the value of the i-th field (including nested groups/components) in the rawMessage
    int[] valueLengths
    // rawMessage[valuePositions[i] + valueLengths[i] - 1] is the last byte of the value of the i-th field (including nested groups/components) in the rawMessage
) {
    /**
     * Finds the index of a specific tag in the tags array.
     *
     * @param tag The tag number to search for
     * @return The index of the tag, or -1 if not found
     */
    int indexOfTag(int tag, int start, int end) {
        for (int i = start; i < end && i < tags.length; i++) {
            if (tags[i] == tag) {
                return i;
            }
        }
        return -1;
    }
}