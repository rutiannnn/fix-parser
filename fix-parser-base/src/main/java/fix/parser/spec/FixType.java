package fix.parser.spec;

public enum FixType {
    STRING("String"),
    CHAR("char"),
    PRICE("double"),
    INT("int"),
    AMT("double"),
    QTY("double"),
    CURRENCY("String"),
    MULTIPLEVALUESTRING("String"),
    EXCHANGE("String"),
    UTCTIMESTAMP("java.time.Instant"),
    BOOLEAN("boolean"),
    LOCALMKTDATE("java.time.LocalDate"),
    FLOAT("double"),
    PRICEOFFSET("double"),
    MONTHYEAR("java.time.YearMonth"),
    UTCDATEONLY("java.time.LocalDate"),
    UTCTIMEONLY("java.time.LocalTime"),
    NUMINGROUP("int"),
    PERCENTAGE("double"),
    SEQNUM("int"),
    LENGTH("int"),
    COUNTRY("String"),
    TIME("java.time.LocalTime"),
    DATE("java.time.LocalDate"),
    DATA("byte[]"),
    XMLDATA("String"),
    LANGUAGE("String");

    private final String javaType;

    FixType(String javaType) {
        this.javaType = javaType;
    }

    public String getJavaType() {
        return javaType;
    }

    public static FixType fromString(String fixType) {
        try {
            return valueOf(fixType);
        } catch (IllegalArgumentException e) {
            // Default to STRING if type is not recognized
            return STRING;
        }
    }
}