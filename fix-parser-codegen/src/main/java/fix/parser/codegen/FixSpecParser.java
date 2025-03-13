package fix.parser.codegen;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FixSpecParser {
    private final Document document;
    private final Map<String, FieldDef> fieldMap;
    private final Map<String, ComponentDef> componentMap;

    public FixSpecParser(File xmlFile) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        this.document = builder.parse(xmlFile);
        this.fieldMap = new HashMap<>();
        this.componentMap = new HashMap<>();
    }

    public FixSpec parse() {
        Element root = document.getDocumentElement();
        String major = root.getAttribute("major");
        String minor = root.getAttribute("minor");

        buildFieldMap(root);
        buildComponentMap(root);

        NodeList headerNodes = root.getElementsByTagName("header");
        NodeList trailerNodes = root.getElementsByTagName("trailer");
        NodeList messagesNodes = root.getElementsByTagName("messages");

        MessageSection header = null;
        if (headerNodes.getLength() > 0) {
            header = parseSection((Element) headerNodes.item(0));
        }

        MessageSection trailer = null;
        if (trailerNodes.getLength() > 0) {
            trailer = parseSection((Element) trailerNodes.item(0));
        }

        List<MessageDef> messages = new ArrayList<>();
        if (messagesNodes.getLength() > 0) {
            Element messagesElement = (Element) messagesNodes.item(0);
            NodeList messageNodes = messagesElement.getElementsByTagName("message");
            for (int i = 0; i < messageNodes.getLength(); i++) {
                Element messageElement = (Element) messageNodes.item(i);
                messages.add(parseMessage(messageElement));
            }
        }

        return new FixSpec(major, minor, header, trailer, messages, componentMap, fieldMap);
    }

    private void buildFieldMap(Element root) {
        NodeList fieldsNodes = root.getElementsByTagName("fields");
        if (fieldsNodes.getLength() > 0) {
            Element fieldsElement = (Element) fieldsNodes.item(0);
            NodeList fieldNodes = fieldsElement.getElementsByTagName("field");
            for (int i = 0; i < fieldNodes.getLength(); i++) {
                Element fieldElement = (Element) fieldNodes.item(i);
                String name = fieldElement.getAttribute("name");
                int number = Integer.parseInt(fieldElement.getAttribute("number"));
                FixType type = FixType.fromString(fieldElement.getAttribute("type"));
                fieldMap.put(name, new FieldDef(number, name, type, false)); // default required to false
            }
        }
    }

    private void buildComponentMap(Element root) {
        NodeList componentsNodes = root.getElementsByTagName("components");
        if (componentsNodes.getLength() > 0) {
            Element componentsElement = (Element) componentsNodes.item(0);
            NodeList componentNodes = componentsElement.getElementsByTagName("component");
            for (int i = 0; i < componentNodes.getLength(); i++) {
                Element componentElement = (Element) componentNodes.item(i);
                String name = componentElement.getAttribute("name");
                ComponentDef component = parseComponent(componentElement);
                componentMap.put(name, component);
            }
        }
    }

    private ComponentDef parseComponent(Element element) {
        String name = element.getAttribute("name");
        List<FieldDef> fields = new ArrayList<>();
        List<GroupDef> groups = new ArrayList<>();
        List<ComponentRef> components = new ArrayList<>();

        NodeList fieldNodes = element.getElementsByTagName("field");
        for (int i = 0; i < fieldNodes.getLength(); i++) {
            Element fieldElement = (Element) fieldNodes.item(i);
            fields.add(parseField(fieldElement));
        }

        NodeList groupNodes = element.getElementsByTagName("group");
        for (int i = 0; i < groupNodes.getLength(); i++) {
            Element groupElement = (Element) groupNodes.item(i);
            groups.add(parseGroup(groupElement));
        }

        NodeList componentNodes = element.getElementsByTagName("component");
        for (int i = 0; i < componentNodes.getLength(); i++) {
            Element componentElement = (Element) componentNodes.item(i);
            String componentName = componentElement.getAttribute("name");
            boolean required = "Y".equals(componentElement.getAttribute("required"));
            components.add(new ComponentRef(componentName, required));
        }

        return new ComponentDef(name, fields, groups, components);
    }

    private MessageSection parseSection(Element element) {
        List<FieldDef> fields = new ArrayList<>();
        List<GroupDef> groups = new ArrayList<>();

        NodeList fieldNodes = element.getElementsByTagName("field");
        for (int i = 0; i < fieldNodes.getLength(); i++) {
            Element fieldElement = (Element) fieldNodes.item(i);
            fields.add(parseField(fieldElement));
        }

        NodeList groupNodes = element.getElementsByTagName("group");
        for (int i = 0; i < groupNodes.getLength(); i++) {
            Element groupElement = (Element) groupNodes.item(i);
            groups.add(parseGroup(groupElement));
        }

        NodeList componentNodes = element.getElementsByTagName("component");
        for (int i = 0; i < componentNodes.getLength(); i++) {
            Element componentElement = (Element) componentNodes.item(i);
            String name = componentElement.getAttribute("name");
            ComponentDef componentDef = componentMap.get(name);
            if (componentDef == null) {
                throw new IllegalStateException("Component not found: " + name);
            }
            fields.addAll(componentDef.fields());
            groups.addAll(componentDef.groups());
        }

        return new MessageSection(fields, groups);
    }

    private MessageDef parseMessage(Element element) {
        String name = element.getAttribute("name");
        String msgtype = element.getAttribute("msgtype");
        String msgcat = element.getAttribute("msgcat");

        MessageSection section = parseSection(element);
        if (name.equals("Reject")) {
            System.out.printf("%s\n", section);
        }
        return new MessageDef(name, msgtype, msgcat, section.fields(), section.groups());
    }

    private FieldDef parseField(Element element) {
        String name = element.getAttribute("name");
        boolean required = "Y".equals(element.getAttribute("required"));

        FieldDef baseField = fieldMap.get(name);
        if (baseField == null) {
            throw new IllegalStateException("Field not found: " + name);
        }

        return new FieldDef(baseField.number(), name, baseField.type(), required);
    }

    private GroupDef parseGroup(Element element) {
        String name = element.getAttribute("name");
        boolean required = "Y".equals(element.getAttribute("required"));

        MessageSection section = parseSection(element);
        return new GroupDef(name, required, section.fields(), section.groups());
    }

}
