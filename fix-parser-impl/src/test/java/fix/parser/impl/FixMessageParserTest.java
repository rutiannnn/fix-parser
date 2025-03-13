package fix.parser.impl;

import fix.parser.message.base.FixMessage;
import fix.parser.messages44.NewOrderSingleMessage;
import fix.parser.messages44.NoPartyIDsGroup;
import fix.parser.messages44.UserRequestMessage;
import fix.parser.spec.FixSpec;
import fix.parser.spec.FixSpecParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class FixMessageParserTest {
    private static FixMessageParser parser;
    private static final String BASE_PACKAGE = "fix.parser.messages44";

    @BeforeAll
    static void setUp() throws Exception {
        File specFile = new File("../fix-parser-messages44/src/main/resources/FIX44.xml");
        FixSpecParser specParser = new FixSpecParser(specFile);
        FixSpec spec = specParser.parse();
        parser = new FixMessageParser(spec, BASE_PACKAGE);
    }

    @Test
    @DisplayName("Should parse a complete FIX message")
    void testParseCompleteMessage() {
        // Given
        String fixMessage =
            "8=FIX.4.4\u00019=176\u000135=D\u000149=BUYER\u000156=SELLER\u000134=1\u0001" +
                "52=20230615-14:30:00.000\u000111=123456\u000155=IBM\u000154=1\u000144=150.25\u0001" +
                "38=1000\u000110=128\u0001";
        byte[] messageBytes = fixMessage.getBytes(StandardCharsets.ISO_8859_1);

        // When
        FixMessage actual = parser.parse(messageBytes);
        assertInstanceOf(NewOrderSingleMessage.class, actual);
        NewOrderSingleMessage msg = (NewOrderSingleMessage) actual;

        // Then
        assertEquals("FIX.4.4", msg.getHeader().getBeginString());  // BeginString
        assertEquals(176, msg.getHeader().getBodyLength());      // BodyLength
        assertEquals("D", msg.getHeader().getMsgType());          // MsgType
        assertEquals("BUYER", msg.getHeader().getSenderCompID()); // SenderCompID
        assertEquals("SELLER", msg.getHeader().getTargetCompID());// TargetCompID
        assertEquals(1, msg.getHeader().getMsgSeqNum());        // MsgSeqNum
        assertEquals("123456", msg.getClOrdID());                 // ClOrdID
        assertEquals("IBM", msg.getInstrument().getSymbol());                     // Symbol
        assertEquals(1, msg.getSide());                         // Side
        assertEquals(150.25, msg.getPrice(), 0.001);             // Price
        assertEquals(1000, msg.getOrderQtyData().getOrderQty());                    // OrderQty
    }

    @Test
    @DisplayName("Should parse message with optional fields")
    void testParseMessageWithOptionalFields() {
        // Given
        String fixMessage = """
            8=FIX.4.4\u00019=100\u000135=D\u000149=BUYER\u000156=SELLER\u000134=1\u0001\
            52=20230615-14:30:00.000\u000111=123456\u000155=IBM\u000154=1\u000138=1000\u0001\
            40=2\u000159=0\u000110=128\u0001""";
        byte[] messageBytes = fixMessage.getBytes(StandardCharsets.ISO_8859_1);

        // When
        NewOrderSingleMessage message = (NewOrderSingleMessage) parser.parse(messageBytes);

        // Then
        assertEquals("123456", message.getClOrdID());
        assertEquals("IBM", message.getInstrument().getSymbol());
        assertEquals('2', message.getOrdType());
        assertEquals('0', message.getTimeInForce());
    }

    @Test
    @DisplayName("Should cache message classes")
    void testMessageClassCaching() {
        // Given
        String fixMessage = """
            8=FIX.4.4\u00019=176\u000135=D\u000149=BUYER\u000156=SELLER\u000134=1\u0001\
            52=20230615-14:30:00.000\u000111=123456\u000155=IBM\u000154=1\u000144=150.25\u0001\
            38=1000\u000110=128\u0001""";
        byte[] messageBytes = fixMessage.getBytes(StandardCharsets.ISO_8859_1);

        // When
        FixMessage message1 = parser.parse(messageBytes);
        FixMessage message2 = parser.parse(messageBytes);

        // Then
        assertSame(message1.getClass(), message2.getClass());
    }

    @Test
    @DisplayName("Should parse message with repeating groups")
    void testParseMessageWithRepeatingGroups() {
        // Given
        String fixMessage = """
            8=FIX.4.4\u00019=200\u000135=D\u000149=BUYER\u000156=SELLER\u000134=1\u0001\
            52=20230615-14:30:00.000\u000111=123456\u000155=IBM\u000154=1\u000144=150.25\u0001\
            38=1000\u0001453=2\u0001448=TRADER1\u0001447=D\u0001452=1\u0001\
            448=TRADER2\u0001447=D\u0001452=2\u000110=128\u0001""";
        byte[] messageBytes = fixMessage.getBytes(StandardCharsets.ISO_8859_1);

        // When
        NewOrderSingleMessage message = (NewOrderSingleMessage) parser.parse(messageBytes);

        // Then
        NoPartyIDsGroup[] parties = message.getParties().getPartyIDs();
        assertEquals(2, parties.length);

        assertEquals("TRADER1", parties[0].getPartyID());
        assertEquals('D', parties[0].getPartyIDSource());
        assertEquals(1, parties[0].getPartyRole());

        // Verify second group instance
        assertEquals("TRADER2", parties[1].getPartyID());
        assertEquals('D', parties[1].getPartyIDSource());
        assertEquals(2, parties[1].getPartyRole());
    }

    @Test
    @DisplayName("Should parse UserRequest message with RawData")
    void testParseUserRequestMessage() {
        // Given
        // Create a UserRequest (BE) message with both required and optional fields
        // Including RawData with binary content containing field separator
        String fixMessage = """
            8=FIX.4.4\u00019=178\u000135=BE\u000149=SENDER\u000156=TARGET\u000134=15\u0001\
            52=20240115-12:34:56.789\u0001923=REQ12345\u0001924=1\u0001553=testuser\u0001\
            554=password123\u0001925=newpass456\u000195=14\u0001\
            96=binary\u0001content\u0001\
            10=123\u0001""";
        byte[] messageBytes = fixMessage.getBytes(StandardCharsets.ISO_8859_1);

        // When
        UserRequestMessage message = (UserRequestMessage) parser.parse(messageBytes);

        // Then
        // Verify header fields
        assertEquals("FIX.4.4", message.getHeader().getBeginString());
        assertEquals("BE", message.getHeader().getMsgType());
        assertEquals("SENDER", message.getHeader().getSenderCompID());
        assertEquals("TARGET", message.getHeader().getTargetCompID());
        assertEquals(15, message.getHeader().getMsgSeqNum());

        // Verify required fields
        assertEquals("REQ12345", message.getUserRequestID());
        assertEquals(1, message.getUserRequestType());  // 1 = Request status
        assertEquals("testuser", message.getUsername());

        // Verify optional fields
        assertEquals("password123", message.getPassword());
        assertEquals("newpass456", message.getNewPassword());

        // Verify RawData field and its length
        assertEquals(14, message.getRawDataLength());
        assertArrayEquals(
            "binary\u0001content".getBytes(StandardCharsets.ISO_8859_1),
            message.getRawData()
        );
        assertEquals("123", message.getTrailer().getCheckSum());
    }

    @Test
    @DisplayName("Should parse UserRequest message with minimum required fields")
    void testParseUserRequestMessageMinimal() {
        // Given
        // Create a UserRequest (BE) message with only required fields
        String fixMessage = """
            8=FIX.4.4\u00019=103\u000135=BE\u000149=SENDER\u000156=TARGET\u000134=16\u0001\
            52=20240115-12:34:56.789\u0001923=REQ12346\u0001924=1\u0001553=testuser\u0001\
            10=123\u0001""";
        byte[] messageBytes = fixMessage.getBytes(StandardCharsets.US_ASCII);

        // When
        UserRequestMessage message = (UserRequestMessage) parser.parse(messageBytes);

        // Then
        // Verify required fields
        assertEquals("REQ12346", message.getUserRequestID());
        assertEquals(1, message.getUserRequestType());
        assertEquals("testuser", message.getUsername());
    }
}
