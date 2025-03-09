package fix.parser.demo;

import fix.parser.FixParser;
import fix.parser.api.FixMessage;

public class FixParserDemo {
    // Example FIX messages
    private static final String NEW_ORDER_SINGLE =
            "8=FIX.4.2\u00019=153\u000135=D\u000134=2\u000149=SENDER\u0001" +
                    "52=20240115-10:31:22\u000156=TARGET\u000111=order123\u000121=1\u0001" +
                    "55=AAPL\u000154=1\u000140=2\u000159=0\u000144=155.50\u000138=100\u0001";

    private static final String EXECUTION_REPORT =
            "8=FIX.4.2\u00019=200\u000135=8\u000134=3\u000149=TARGET\u0001" +
                    "52=20240115-10:31:23\u000156=SENDER\u000111=order123\u000117=exec456\u0001" +
                    "20=0\u000139=2\u000155=AAPL\u000154=1\u000138=100\u000144=155.50\u0001" +
                    "32=100\u000131=155.50\u0001150=2\u000114=100\u00016=155.50\u0001";

    public static void main(String[] args) {
        demonstrateNewOrderParsing();
        System.out.println("\n" + "=".repeat(50) + "\n");
        demonstrateExecutionReportParsing();
    }

    private static void demonstrateNewOrderParsing() {
        System.out.println("Demonstrating New Order Single parsing:");

        // Parse the message
        FixMessage message = FixParser.parse(NEW_ORDER_SINGLE.getBytes());

        // Extract and display common fields
        System.out.println("Message Type (35): " + message.getString(35));
        System.out.println("Sender (49): " + message.getString(49));
        System.out.println("Target (56): " + message.getString(56));

        // Extract order details
        System.out.println("\nOrder Details:");
        System.out.println("Order ID (11): " + message.getString(11));
        System.out.println("Symbol (55): " + message.getString(55));
        System.out.println("Side (54): " + message.getString(54) + " (1=Buy)");
        System.out.println("OrderType (40): " + message.getString(40) + " (2=Limit)");
        System.out.println("Price (44): " + message.getDouble(44));
        System.out.println("Quantity (38): " + message.getInt(38));

        // Demonstrate tag existence check
        System.out.println("\nField presence checks:");
        System.out.println("Has Price (44)? " + message.hasTag(44));
        System.out.println("Has StopPx (99)? " + message.hasTag(99));
    }

    private static void demonstrateExecutionReportParsing() {
        System.out.println("Demonstrating Execution Report parsing:");

        // Parse the message
        FixMessage message = FixParser.parse(EXECUTION_REPORT.getBytes());

        // Extract and display execution details
        System.out.println("Message Type (35): " + message.getString(35));
        System.out.println("ExecID (17): " + message.getString(17));
        System.out.println("OrderID (11): " + message.getString(11));

        // Extract execution details
        System.out.println("\nExecution Details:");
        System.out.println("Symbol (55): " + message.getString(55));
        System.out.println("ExecType (150): " + message.getString(150) + " (2=Fill)");
        System.out.println("OrdStatus (39): " + message.getString(39) + " (2=Filled)");
        System.out.println("LastQty (32): " + message.getInt(32));
        System.out.println("LastPx (31): " + message.getDouble(31));
        System.out.println("CumQty (14): " + message.getInt(14));
        System.out.println("AvgPx (6): " + message.getDouble(6));

        // Demonstrate raw value access
        System.out.println("\nRaw value access example:");
        byte[] rawPrice = message.getRawValue(44);
        System.out.println("Price (44) as raw bytes: " + new String(rawPrice));
    }
}