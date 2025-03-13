package fix.parser.spec;

import java.util.List;

public record ComponentDef(
    String name,
    List<FieldDef> fields,
    List<GroupDef> groups,
    List<ComponentRef> components
) {
}
