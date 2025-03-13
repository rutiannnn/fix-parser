package fix.parser.spec;

import java.util.List;

public record MessageDef(
    String name,
    String msgtype,
    String msgcat,
    List<FieldDef> fields,
    List<GroupDef> groups,
    List<ComponentRef> components
) {
}
