package fix.parser;

import fix.parser.api.FixMessage;
import fix.parser.api.MissingTagException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class FixParserSingleOrderTest {

    private static final String SAMPLE_ORDER =
            "8=FIX.4.2\u00019=153\u000135=D\u000134=2\u000149=SENDER\u0001" +
                    "52=20240115-10:31:22\u000156=TARGET\u000111=order123\u000121=1\u0001" +
                    "55=AAPL\u000154=1\u000140=2\u000159=0\u000144=155.50\u000138=100\u0001";

    @Test
    @DisplayName("Should parse FIX message and extract fields correctly")
    public void testParseFixMessage() {
        // When
        FixMessage message = FixParser.parse(SAMPLE_ORDER.getBytes(StandardCharsets.ISO_8859_1));

        // Then
        // Header fields
        assertEquals("FIX.4.2", message.getString(8), "BeginString should match");
        assertEquals("153", message.getString(9), "BodyLength should match");
        assertEquals("D", message.getString(35), "MsgType should be NewOrderSingle");

        // Routing fields
        assertEquals("SENDER", message.getString(49), "SenderCompID should match");
        assertEquals("TARGET", message.getString(56), "TargetCompID should match");
        assertEquals("2", message.getString(34), "MsgSeqNum should match");
        assertEquals("20240115-10:31:22", message.getString(52), "SendingTime should match");

        // Order fields
        assertEquals("order123", message.getString(11), "ClOrdID should match");
        assertEquals("1", message.getString(21), "HandlInst should match");
        assertEquals("AAPL", message.getString(55), "Symbol should match");
        assertEquals("1", message.getString(54), "Side should be Buy");
        assertEquals("2", message.getString(40), "OrdType should be Limit");
        assertEquals("0", message.getString(59), "TimeInForce should be Day");
        assertEquals(155.50, message.getDouble(44), 0.001, "Price should match");
        assertEquals(100, message.getInt(38), "OrderQty should match");
    }

    @Test
    @DisplayName("Should handle field presence checks correctly")
    public void testFieldPresence() {
        // When
        FixMessage message = FixParser.parse(SAMPLE_ORDER.getBytes(StandardCharsets.ISO_8859_1));

        // Then
        assertTrue(message.hasTag(44), "Should have Price field");
        assertTrue(message.hasTag(38), "Should have OrderQty field");
        assertFalse(message.hasTag(99), "Should not have StopPx field");
        assertFalse(message.hasTag(126), "Should not have ExpireTime field");
    }

    @Test
    @DisplayName("Should handle different field types correctly")
    public void testFieldTypes() {
        // When
        FixMessage message = FixParser.parse(SAMPLE_ORDER.getBytes());

        // Then
        // String field
        assertEquals("AAPL", message.getString(55));

        // Integer field
        assertEquals(100, message.getInt(38));

        // Double field
        assertEquals(155.50, message.getDouble(44), 0.001);

        // Raw byte access
        assertArrayEquals("155.50".getBytes(), message.getRawValue(44));
    }

    @Test
    @DisplayName("Should throw exception for missing fields")
    public void testMissingFields() {
        // When
        FixMessage message = FixParser.parse(SAMPLE_ORDER.getBytes(StandardCharsets.ISO_8859_1));

        // Then
        assertThrows(MissingTagException.class, () -> message.getString(999),
                "Should throw exception for non-existent field");
        assertThrows(MissingTagException.class, () -> message.getInt(999),
                "Should throw exception for non-existent field");
        assertThrows(MissingTagException.class, () -> message.getDouble(999),
                "Should throw exception for non-existent field");
    }

    @Test
    @DisplayName("Should handle empty message")
    public void testEmptyMessage() {
        // Empty message with just BeginString and BodyLength
        String emptyMsg = "8=FIX.4.2\u00019=0\u0001";

        // When
        FixMessage message = FixParser.parse(emptyMsg.getBytes(StandardCharsets.ISO_8859_1));

        // Then
        assertEquals("FIX.4.2", message.getString(8), "BeginString should match");
        assertEquals("0", message.getString(9), "BodyLength should match");
        assertFalse(message.hasTag(35), "Should not have MsgType");
    }
}