package fix.parser.spec;

import java.util.List;
import java.util.Map;

public record FixSpec(
    String major,
    String minor,
    MessageSection header,
    MessageSection trailer,
    List<MessageDef> messages,
    Map<String, ComponentDef> components,
    Map<String, FieldDef> fields
) {
}

