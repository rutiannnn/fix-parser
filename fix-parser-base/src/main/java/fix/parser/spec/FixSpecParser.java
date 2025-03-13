package fix.parser.spec;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
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
    private final Map<Integer, FieldDef> fieldMapByNumber;
    private final Map<String, ComponentDef> componentMap;

    public FixSpecParser(File xmlFile) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        this.document = builder.parse(xmlFile);
        this.fieldMap = new HashMap<>();
        this.fieldMapByNumber = new HashMap<>();
        this.componentMap = new HashMap<>();
    }

    public FixSpec parse() {
        Element root = document.getDocumentElement();
        String major = root.getAttribute("major");
        String minor = root.getAttribute("minor");

        buildFieldMap(root);
        buildComponentMap(root);

        MessageSection header = null;
        NodeList headerNodes = root.getElementsByTagName("header");
        if (headerNodes.getLength() > 0) {
            header = parseSection((Element) headerNodes.item(0));
        }

        MessageSection trailer = null;
        NodeList trailerNodes = root.getElementsByTagName("trailer");
        if (trailerNodes.getLength() > 0) {
            trailer = parseSection((Element) trailerNodes.item(0));
        }

        List<MessageDef> messages = new ArrayList<>();
        NodeList messagesNodes = root.getElementsByTagName("messages");
        if (messagesNodes.getLength() > 0) {
            Element messagesElement = (Element) messagesNodes.item(0);
            NodeList childNodes = messagesElement.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                Node node = childNodes.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE && "message".equals(node.getNodeName())) {
                    messages.add(parseMessage((Element) node));
                }
            }
        }

        return new FixSpec(major, minor, header, trailer, messages, componentMap, fieldMap, fieldMapByNumber);
    }

    private void buildFieldMap(Element root) {
        NodeList fieldsNodes = root.getElementsByTagName("fields");
        if (fieldsNodes.getLength() > 0) {
            Element fieldsElement = (Element) fieldsNodes.item(0);
            NodeList childNodes = fieldsElement.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                Node node = childNodes.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE && "field".equals(node.getNodeName())) {
                    Element fieldElement = (Element) node;
                    String name = fieldElement.getAttribute("name");
                    int number = Integer.parseInt(fieldElement.getAttribute("number"));
                    FixType type = FixType.fromString(fieldElement.getAttribute("type"));
                    fieldMap.put(name, new FieldDef(number, name, type));
                    fieldMapByNumber.put(number, new FieldDef(number, name, type));
                }
            }
        }
    }

    private void buildComponentMap(Element root) {
        NodeList componentsNodes = root.getElementsByTagName("components");
        if (componentsNodes.getLength() > 0) {
            Element componentsElement = (Element) componentsNodes.item(0);
            NodeList childNodes = componentsElement.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                Node node = childNodes.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE && "component".equals(node.getNodeName())) {
                    Element componentElement = (Element) node;
                    String name = componentElement.getAttribute("name");
                    ComponentDef component = parseComponent(name, componentElement);
                    componentMap.put(name, component);
                }
            }
        }
    }

    private ComponentDef parseComponent(String name, Element element) {
        List<FieldDef> fields = new ArrayList<>();
        List<GroupDef> groups = new ArrayList<>();
        List<ComponentRef> components = new ArrayList<>();

        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element childElement = (Element) node;
                switch (node.getNodeName()) {
                    case "field" -> fields.add(parseField(childElement));
                    case "group" -> groups.add(parseGroup(childElement));
                    case "component" -> components.add(new ComponentRef(childElement.getAttribute("name")));
                }
            }
        }

        return new ComponentDef(name, fields, groups, components);
    }

    private MessageSection parseSection(Element element) {
        List<FieldDef> fields = new ArrayList<>();
        List<GroupDef> groups = new ArrayList<>();

        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element childElement = (Element) node;
                switch (node.getNodeName()) {
                    case "field" -> fields.add(parseField(childElement));
                    case "group" -> groups.add(parseGroup(childElement));
                }
            }
        }

        return new MessageSection(fields, groups);
    }

    private MessageDef parseMessage(Element element) {
        String name = element.getAttribute("name");
        String msgtype = element.getAttribute("msgtype");
        String msgcat = element.getAttribute("msgcat");

        List<FieldDef> fields = new ArrayList<>();
        List<GroupDef> groups = new ArrayList<>();
        List<ComponentRef> components = new ArrayList<>();

        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element childElement = (Element) node;
                switch (node.getNodeName()) {
                    case "field" -> fields.add(parseField(childElement));
                    case "group" -> groups.add(parseGroup(childElement));
                    case "component" -> components.add(new ComponentRef(childElement.getAttribute("name")));
                }
            }
        }

        return new MessageDef(name, msgtype, msgcat, fields, groups, components);
    }

    private FieldDef parseField(Element element) {
        return fieldMap.get(element.getAttribute("name"));
    }

    private GroupDef parseGroup(Element element) {
        String name = element.getAttribute("name");

        List<FieldDef> fields = new ArrayList<>();
        List<GroupDef> groups = new ArrayList<>();
        List<ComponentRef> components = new ArrayList<>();

        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element childElement = (Element) node;
                switch (node.getNodeName()) {
                    case "field" -> fields.add(parseField(childElement));
                    case "group" -> groups.add(parseGroup(childElement));
                    case "component" -> components.add(new ComponentRef(childElement.getAttribute("name")));
                }
            }
        }

        return new GroupDef(name, fields, groups, components);
    }

}
