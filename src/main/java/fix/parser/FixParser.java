package fix.parser;

import fix.parser.api.FixMessage;

public class FixParser {

    // Parse a FIX message from byte array
    public static FixMessage parse(byte[] message) {
        return new FixMessageImpl(message);
    }
}