# fix-parser

## Overview

A high-performance FIX (Financial Information eXchange) protocol message parser implemented in Java. The parser is
designed
for low-latency trading systems, focusing on efficient parsing and minimal object allocation.

## Key Features

- Zero-copy parsing of FIX messages
- Direct byte array access for maximum performance
- Support for common FIX data types (String, Int, Double)
- Field presence checking
- Raw value access for custom handling

## Technical Details

### Implementation Assumptions

1. Message Format
   - FIX 4.2 protocol support
   - Fields are delimited by SOH (0x01) character
   - Tag-value pairs are separated by '=' (0x3d) character
   - Messages are well-formed (basic validation only)
   - Messages follow [FIX TagValue Encoding][fix-tag-value-encoding] so [Simple Binary Encoding (SBE)][fix-sbe] is not
     supported

2. Performance Considerations
   - Pre-allocated arrays for tag positions
   - Minimal String object creation
   - Direct byte array manipulation
   - Integer parsing without intermediate String conversion

3. Memory Usage
   - Stores original message bytes
   - Index arrays for quick field lookup
   - No hash maps or dynamic collections

### Design Decisions

1. API Design
   - Simple interface with essential methods
   - Type-specific getters (getString, getInt, getDouble)
   - Raw byte access for custom handling

2. Error Handling
   - Runtime exceptions for missing fields
   - Null returns for optional fields
   - No complex validation (assumed to be handled upstream)

3. Thread Safety
   - Immutable message representation
   - Thread-safe after construction
   - No shared state between parser instances

### TODOs and Open Questions

1. Do I need to support all versions of FIX?
1. Do I need to support FIX SBE?
1. Will I get performance gain from parallelism?
1. How to support repeating group?
1. How to visualize the jfr file from benchmark?

## Usage Example

```java
String fixMsg = "8=FIX.4.2\u00019=153\u000135=D\u0001...";
FixMessage message = FixParser.parse(fixMsg.getBytes());

// Access fields
String symbol = message.getString(55);  // Symbol
double price = message.getDouble(44);   // Price
int quantity = message.getInt(38);      // Quantity
```

## Requirements

- Java 21 or higher
- Maven 3.8.x or higher

## Building

```bash
mvn clean install
```

## References

### FIX Official Documentation

1. [FIX 4.2 Specification][fix-4-2]
1. [FIX Simple Binary Encoding (SBE)][fix-sbe]
1. [FIX TagValue Encoding][fix-tag-value-encoding]

### YouTube Videos

1. [FIX protocol (explained by a quant developer)][youtube-fix-explained]

### AI Tools Used

1. [Augment Code][augment-code]
1. [Microsoft Copilot][copilot]

### Existing FIX Parsers

1. [QuickFIX/J][quickfixj]

[augment-code]: https://docs.augmentcode.com/introduction

[copilot]: https://copilot.microsoft.com/

[fix-4-2]: https://www.fixtrading.org/standards/fix-4-2/]

[fix-sbe]: https://www.fixtrading.org/standards/sbe/

[fix-tag-value-encoding]: https://www.fixtrading.org/standards/tagvalue-online/?form=MG0AV3&form=MG0AV3

[quickfixj]: https://github.com/quickfix-j/quickfixj

[youtube-fix-explained]: https://youtu.be/uZ8UEVhtPAo?si=uowAtfj_vtHk-uNp
