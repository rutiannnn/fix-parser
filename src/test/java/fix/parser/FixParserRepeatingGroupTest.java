package fix.parser;

import fix.parser.api.FixMessage;
import fix.parser.api.MissingTagException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class FixParserRepeatingGroupTest {

    private static final String MESSAGE_WITH_GROUPS =
            "8=FIX.4.2\u00019=200\u000135=D\u000134=2\u000149=SENDER\u0001" +
                    "52=20240115-10:31:22\u000156=TARGET\u000111=order123\u0001" +
                    // NoPartyIDs (453) group with 2 entries
                    "453=2\u0001" +  // Number of party IDs
                    "448=PARTY1\u0001447=D\u0001452=1\u0001" +  // First party
                    "448=PARTY2\u0001447=B\u0001452=2\u0001" +  // Second party
                    // Order details
                    "55=AAPL\u000154=1\u000140=2\u000159=0\u000144=155.50\u000138=100\u0001";

    private static final String MESSAGE_WITH_NESTED_GROUPS =
            "8=FIX.4.2\u00019=300\u000135=8\u000134=2\u000149=SENDER\u0001" +
                    "52=20240115-10:31:22\u000156=TARGET\u000111=order123\u0001" +
                    // NoAllocs (78) group with 2 entries
                    "78=2\u0001" +  // Number of allocations
                    "79=ACCOUNT1\u000180=100\u0001" +  // First allocation
                    // Nested NoNestedPartyIDs (539) within first allocation
                    "539=2\u0001" +  // Number of nested parties
                    "524=NEST1\u0001525=A\u0001" +  // First nested party
                    "524=NEST2\u0001525=B\u0001" +  // Second nested party
                    // Second allocation
                    "79=ACCOUNT2\u000180=200\u0001" +
                    "539=1\u0001" +  // One nested party
                    "524=NEST3\u0001525=C\u0001";  // Nested party for second allocation

    @Test
    @DisplayName("Should parse simple repeating groups correctly")
    public void testSimpleGroups() {
        // When
        FixMessage message = FixParser.parse(MESSAGE_WITH_GROUPS.getBytes(StandardCharsets.ISO_8859_1));
        int[] partyTags = {448, 447, 452}; // PartyID, PartyRole, PartyIDSource
        FixMessage[] parties = message.getGroups(453, partyTags);  // NoPartyIDs

        // Then
        assertEquals(2, parties.length, "Should have 2 party entries");

        // First party
        assertEquals("PARTY1", parties[0].getString(448), "First party ID");
        assertEquals("D", parties[0].getString(447), "First party role");
        assertEquals("1", parties[0].getString(452), "First party qualifier");

        // Second party
        assertEquals("PARTY2", parties[1].getString(448), "Second party ID");
        assertEquals("B", parties[1].getString(447), "Second party role");
        assertEquals("2", parties[1].getString(452), "Second party qualifier");
    }

    @Test
    @DisplayName("Should parse nested repeating groups correctly")
    public void testNestedGroups() {
        // When
        FixMessage message = FixParser.parse(MESSAGE_WITH_NESTED_GROUPS.getBytes(StandardCharsets.ISO_8859_1));
        int[] allocTags = {79, 80, 539, 524, 525}; // Account, AllocQty, NoNestedPartyIDs, NestedPartyID, NestedPartyRole
        FixMessage[] allocations = message.getGroups(78, allocTags);  // NoAllocs

        // Then
        assertEquals(2, allocations.length, "Should have 2 allocation entries");

        // First allocation
        assertEquals("ACCOUNT1", allocations[0].getString(79), "First account");
        assertEquals("100", allocations[0].getString(80), "First allocation amount");

        // Nested parties in first allocation
        int[] nestedPartyTags = {524, 525}; // NestedPartyID, NestedPartyRole
        FixMessage[] nestedParties1 = allocations[0].getGroups(539, nestedPartyTags);  // NoNestedPartyIDs
        assertEquals(2, nestedParties1.length, "First allocation should have 2 nested parties");
        assertEquals("NEST1", nestedParties1[0].getString(524), "First nested party ID");
        assertEquals("NEST2", nestedParties1[1].getString(524), "Second nested party ID");

        // Second allocation
        assertEquals("ACCOUNT2", allocations[1].getString(79), "Second account");
        assertEquals("200", allocations[1].getString(80), "Second allocation amount");

        // Nested parties in second allocation
        FixMessage[] nestedParties2 = allocations[1].getGroups(539, nestedPartyTags);  // NoNestedPartyIDs
        assertEquals(1, nestedParties2.length, "Second allocation should have 1 nested party");
        assertEquals("NEST3", nestedParties2[0].getString(524), "Third nested party ID");
    }

    @Test
    @DisplayName("Should handle empty groups correctly")
    public void testEmptyGroups() {
        String messageWithEmptyGroup = "8=FIX.4.2\u00019=100\u000135=D\u0001453=0\u000155=AAPL\u0001";
        FixMessage message = FixParser.parse(messageWithEmptyGroup.getBytes(StandardCharsets.ISO_8859_1));

        FixMessage[] groups = message.getGroups(453, new int[]{});
        assertEquals(0, groups.length, "Should return empty array for zero-size group");
    }

    @Test
    @DisplayName("Should handle missing group count tag")
    public void testMissingGroupTag() {
        String messageWithoutGroup = "8=FIX.4.2\u00019=100\u000135=D\u000155=AAPL\u0001";
        FixMessage message = FixParser.parse(messageWithoutGroup.getBytes(StandardCharsets.ISO_8859_1));

        assertThrows(MissingTagException.class, () -> message.getGroups(453, new int[]{}),
                "Should throw exception for missing group tag");
    }
}