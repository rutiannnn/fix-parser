package fix.parser.message.base;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Arrays;

/**
 * Represents a segment of a FIX message, which can be either the main message or a repeating group.
 * Each segment has a reference to the underlying message and maintains its boundaries within that message.
 */
public record Segment(
    UnderlyingMessage rawMessage,  // Underlying FIX message
    int start,                     // Inclusive start position in the raw message
    int end,                       // Exclusive end position in the raw message
    Segment[] segments             // Nested segments (e.g., repeating groups)
) {

    public char getChar(int tag) {
        return getString(tag).charAt(0);
    }

    public String getString(int tagNumber) {
        int index = rawMessage.indexOfTag(tagNumber, start, end);
        return new String(rawMessage.rawMessage(), rawMessage.valuePositions()[index], rawMessage.valueLengths()[index]);
    }

    public int getInt(int tagNumber) {
        return Integer.parseInt(getString(tagNumber));
    }

    public double getDouble(int tagNumber) {
        return Double.parseDouble(getString(tagNumber));
    }

    public boolean getBoolean(int tagNumber) {
        return "Y".equals(getString(tagNumber));
    }

    public Instant getInstant(int tagNumber) {
        return Instant.parse(getString(tagNumber));
    }

    public LocalDate getLocalDate(int tagNumber) {
        return LocalDate.parse(getString(tagNumber));
    }

    public LocalTime getLocalTime(int tagNumber) {
        return LocalTime.parse(getString(tagNumber));
    }

    public YearMonth getYearMonth(int tagNumber) {
        return YearMonth.parse(getString(tagNumber));
    }

    public byte[] getBytes(int tagNumber) {
        int index = rawMessage.indexOfTag(tagNumber, start, end);
        return Arrays.copyOfRange(
            rawMessage.rawMessage(),
            rawMessage.valuePositions()[index],
            rawMessage.valuePositions()[index] + rawMessage.valueLengths()[index]
        );
    }

    public Segment[] getSegments(int tagNumber) {
        int count = 0;
        for (Segment segment : segments) {
            if (segment.rawMessage.tags()[segment.start()] == tagNumber) {
                count++;
            }
        }
        Segment[] matchedSegments = new Segment[count];
        int index = 0;
        for (Segment segment : segments) {
            if (segment.rawMessage.tags()[segment.start()] == tagNumber) {
                matchedSegments[index++] = segment;
            }
        }
        return matchedSegments;
    }
}