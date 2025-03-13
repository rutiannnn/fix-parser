package fix.parser.codegen;

import java.util.List;
import java.util.Map;

record FixSpec(
    String major,
    String minor,
    MessageSection header,
    MessageSection trailer,
    List<MessageDef> messages,
    Map<String, ComponentDef> components
) {
}

record MessageSection(
    List<FieldDef> fields,
    List<GroupDef> groups
) {
}

record MessageDef(
    String name,
    String msgtype,
    String msgcat,
    List<FieldDef> fields,
    List<GroupDef> groups
) {
}

record FieldDef(
    int number,
    String name,
    FixType type,
    boolean required
) {
    String getJavaType() {
        return type.getJavaType();
    }
}

record GroupDef(
    String name,
    boolean required,
    List<FieldDef> fields,
    List<GroupDef> groups
) {
}

record ComponentDef(
    String name,
    List<FieldDef> fields,
    List<GroupDef> groups,
    List<ComponentRef> components
) {
}

record ComponentRef(
    String name,
    boolean required
) {
}