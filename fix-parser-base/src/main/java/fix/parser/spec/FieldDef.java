package fix.parser.spec;

public record FieldDef(
    int number,
    String name,
    FixType type
) {
    public String getJavaType() {
        return type.getJavaType();
    }
}
