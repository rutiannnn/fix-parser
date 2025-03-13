package fix.parser.spec;

import java.util.List;

public record MessageSection(
    List<FieldDef> fields,
    List<GroupDef> groups
) {
}
