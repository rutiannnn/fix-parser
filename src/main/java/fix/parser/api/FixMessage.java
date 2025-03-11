package fix.parser.api;

/**
 * Represents a parsed FIX (Financial Information eXchange) protocol message.
 * This interface provides methods to access fields within a FIX message,
 * supporting different data types and repeating groups.
 *
 * <p>Implementation notes:</p>
 * <ul>
 *   <li>All methods throw {@link MissingTagException} if the requested tag is not found.</li>
 *   <li>Field values are parsed on-demand to optimize performance.</li>
 *   <li>The implementation is thread-safe after construction.</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>
 * FixMessage message = FixParser.parse(bytes);
 *
 * // Access simple fields
 * String symbol = message.getString(55);  // Symbol
 * double price = message.getDouble(44);   // Price
 * int quantity = message.getInt(38);      // OrderQty
 *
 * // Access repeating groups
 * int[] partyTags = {448, 447, 452};     // PartyID, PartyRole, PartyIDSource
 * FixMessage[] parties = message.getGroups(453, partyTags);  // NoPartyIDs
 * </pre>
 *
 * @see MissingTagException
 */
public interface FixMessage {

    /**
     * Retrieves the string value for the specified tag.
     *
     * @param tag the FIX tag number
     * @return the field value as a String
     * @throws MissingTagException if the tag is not found in the message
     */
    String getString(int tag);

    /**
     * Retrieves the integer value for the specified tag.
     *
     * @param tag the FIX tag number
     * @return the field value as an int
     * @throws MissingTagException if the tag is not found in the message
     * @throws NumberFormatException if the field value cannot be parsed as an integer
     */
    int getInt(int tag);

    /**
     * Retrieves the decimal value for the specified tag.
     * Commonly used for price, quantity, and other numeric fields.
     *
     * @param tag the FIX tag number
     * @return the field value as a double
     * @throws MissingTagException if the tag is not found in the message
     * @throws NumberFormatException if the field value cannot be parsed as a double
     */
    double getDouble(int tag);

    /**
     * Checks if a specific tag exists in the message.
     *
     * @param tag the FIX tag number
     * @return true if the tag exists, false otherwise
     */
    boolean hasTag(int tag);

    /**
     * Retrieves the raw byte array value for the specified tag.
     * This method provides direct access to the underlying bytes for maximum performance
     * when custom parsing is needed.
     *
     * @param tag the FIX tag number
     * @return byte array containing the field value
     * @throws MissingTagException if the tag is not found in the message
     */
    byte[] getRawValue(int tag);

    /**
     * Retrieves repeating group entries from the message.
     *
     * @param groupCountTag the tag that specifies the number of entries in the group
     * @param groupTags array of tags that are allowed within this group, including tags from the nested groups
     * @return array of FixMessage objects, each representing one group entry.
     *         Returns an empty array if the group count tag is zero.
     *
     * @throws MissingTagException if the group count tag is not found in the message
     */
    FixMessage[] getGroups(int groupCountTag, int[] groupTags);
}
