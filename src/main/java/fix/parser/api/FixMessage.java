package fix.parser.api;

public interface FixMessage {
    // Get string value for a tag
    String getString(int tag);

    // Get integer value for a tag
    int getInt(int tag);

    // Get decimal value for a tag (price, qty etc.)
    double getDouble(int tag);

    // Check if a tag exists
    boolean hasTag(int tag);

    // Get raw byte array for a tag (for maximum performance when needed)
    byte[] getRawValue(int tag);
}