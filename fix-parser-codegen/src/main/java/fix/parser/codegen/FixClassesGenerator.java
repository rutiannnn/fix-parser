package fix.parser.codegen;

import fix.parser.spec.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

public class FixClassesGenerator {
    private final FixSpec spec;
    private final String packageName;
    private final Path outputDir;
    private final Set<String> generatedGroupClasses = new HashSet<>();

    public FixClassesGenerator(FixSpec spec, String packageName, Path outputDir) {
        this.spec = spec;
        this.packageName = packageName;
        this.outputDir = outputDir;
    }

    public void generate() throws IOException {
        generatedGroupClasses.clear();
        // Create package directory
        Path packageDir = outputDir.resolve(packageName.replace('.', '/'));
        Files.createDirectories(packageDir);

        // Generate field definitions
        generateFieldDefinitions(packageDir);

        // Generate message types
        generateMessageTypes(packageDir);

        // Generate header and trailer classes
        generateHeaderClass(packageDir);
        generateTrailerClass(packageDir);

        // Generate message, component, and group classes
        for (MessageDef message : spec.messages()) {
            generateMessageClass(message, packageDir);
        }

        // Generate component classes
        for (var entry : spec.components().entrySet()) {
            generateComponentClass(entry.getKey(), entry.getValue(), packageDir);
        }
    }

    private void generateFieldDefinitions(Path packageDir) throws IOException {
        Path file = packageDir.resolve("Fields.java");
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(file))) {
            out.print("""
                package %s;
                
                /**
                 * FIX field definitions.
                 * Generated from FIX specification version %s.%s
                 */
                public final class Fields {
                    private Fields() {}
                
                """.formatted(packageName, spec.major(), spec.minor()));

            // Generate constants for all fields in sorted order by field number
            spec.fields().values().stream()
                .sorted(Comparator.comparingInt(FieldDef::number))
                .forEach(field -> generateFieldConstant(out, field));

            out.println("}");
        }
    }

    private static void generateFieldConstant(PrintWriter out, FieldDef field) {
        String constantName = toConstantName(field.name());
        out.printf("    public static final int %s = %d;%n%n",
            constantName, field.number());
    }

    private static String toConstantName(String name) {
        return name.toUpperCase().replace(" ", "_");
    }

    private void generateMessageClass(MessageDef message, Path packageDir) {
        String className = message.name() + "Message";
        Path file = packageDir.resolve(className + ".java");
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(file))) {
            out.print("""
                package %s;
                
                import fix.parser.message.base.FixMessage;
                import fix.parser.message.base.Segment;
                import java.time.Instant;
                import java.time.LocalDate;
                
                public class %s implements FixMessage {
                    private final Segment segment;
                    private final Header header;
                    private final Trailer trailer;
                
                    public %s(Segment segment) {
                        this.segment = segment;
                        this.header = new Header(segment);
                        this.trailer = new Trailer(segment);
                    }
                
                    public Header getHeader() {
                        return header;
                    }
                
                    public Trailer getTrailer() {
                        return trailer;
                    }
                
                """.formatted(packageName, className, className));

            // Generate getters for fields
            for (FieldDef field : message.fields()) {
                generateFieldGetter(out, field);
            }

            // Generate getters for components
            for (ComponentRef component : message.components()) {
                generateComponentGetter(out, component);
            }

            // Generate getters for groups
            for (GroupDef group : message.groups()) {
                generateGroupGetter(out, group, packageDir);
            }

            out.println("}");
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate " + className, e);
        }
    }

    private void generateComponentClass(String name, ComponentDef component, Path packageDir) {
        String className = name + "Component";
        Path file = packageDir.resolve(className + ".java");
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(file))) {
            out.print("""
                package %s;
                
                import fix.parser.message.base.Segment;
                import java.time.Instant;
                import java.time.LocalDate;
                
                public class %s {
                    private final Segment segment;
                
                    public %s(Segment segment) {
                        this.segment = segment;
                    }
                
                """.formatted(packageName, className, className));

            // Generate getters for fields
            for (FieldDef field : component.fields()) {
                generateFieldGetter(out, field);
            }

            // Generate getters for groups
            for (GroupDef group : component.groups()) {
                generateGroupGetter(out, group, packageDir);
            }

            // Generate getters for components
            for (ComponentRef nestedComponent : component.components()) {
                generateComponentGetter(out, nestedComponent);
            }

            out.println("}");
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate " + className, e);
        }
    }

    private void generateGroupClass(String className, GroupDef group, Path packageDir) {
        Path file = packageDir.resolve(className + ".java");
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(file))) {
            out.print("""
                package %s;
                
                import fix.parser.message.base.Segment;
                import java.time.Instant;
                import java.time.LocalDate;
                
                public class %s {
                    private final Segment segment;
                
                    public %s(Segment segment) {
                        this.segment = segment;
                    }
                
                """.formatted(packageName, className, className));

            // Generate getters for fields
            for (FieldDef field : group.fields()) {
                generateFieldGetter(out, field);
            }

            // Generate getters for components
            for (ComponentRef component : group.components()) {
                generateComponentGetter(out, component);
            }

            // Generate getters for nested groups
            for (GroupDef nestedGroup : group.groups()) {
                generateGroupGetter(out, nestedGroup, packageDir);
            }

            out.println("}");
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate " + className, e);
        }
    }

    private static void generateFieldGetter(PrintWriter out, FieldDef field) {
        String methodName = "get" + field.name();
        String javaType = field.getJavaType();
        String getterMethod = switch (field.type()) {
            case STRING, CURRENCY, MULTIPLEVALUESTRING, EXCHANGE, XMLDATA, LANGUAGE, COUNTRY -> "getString";
            case INT, NUMINGROUP, SEQNUM, LENGTH -> "getInt";
            case PRICE, FLOAT, QTY, PRICEOFFSET, AMT, PERCENTAGE -> "getDouble";
            case BOOLEAN -> "getBoolean";
            case UTCTIMESTAMP -> "getInstant";
            case MONTHYEAR -> "getYearMonth";
            case UTCTIMEONLY, TIME -> "getLocalTime";
            case UTCDATEONLY, LOCALMKTDATE, DATE -> "getLocalDate";
            case CHAR -> "getChar";
            case DATA -> "getBytes";
        };

        out.printf("""
            public %s %s() {
                return this.segment.%s(Fields.%s);
            }
            
            """, javaType, methodName, getterMethod, toConstantName(field.name()));
    }

    private void generateGroupGetter(PrintWriter out, GroupDef group, Path packageDir) {
        String groupClassName = group.name() + "Group";

        // Only generate the group class if it hasn't been generated before
        if (generatedGroupClasses.add(groupClassName)) {
            generateGroupClass(groupClassName, group, packageDir);
        }

        out.printf("""                
                public %s[] get%s() {
                    int preceding_idx = segment.rawMessage().indexOfTag(Fields.%s, segment.start(), segment.end());
                    Segment[] segments = segment.getSegments(segment.rawMessage().tags()[preceding_idx + 1]);
                    %s[] result = new %s[segments.length];
                    for (int i = 0; i < segments.length; i++) {
                        result[i] = new %s(segments[i]);
                    }
                    return result;
                }
                
                """,
            groupClassName, group.name().startsWith("No") ? group.name().substring(2) : group.name(),
            toConstantName(group.name()),
            groupClassName, groupClassName,
            groupClassName);
    }

    private void generateMessageTypes(Path packageDir) throws IOException {
        Path file = packageDir.resolve("MessageTypes.java");
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(file))) {
            out.print("""
                package %s;
                
                /**
                 * FIX message type constants.
                 * Generated from FIX specification version %s.%s
                 */
                public final class MessageTypes {
                    private MessageTypes() {}
                
                """.formatted(packageName, spec.major(), spec.minor()));

            for (MessageDef msg : spec.messages()) {
                String constName = toConstantName(msg.name());
                out.printf("""
                    /** %s message type */
                    public static final String %s = "%s";
                    
                    """, msg.name(), constName, msg.msgtype());
            }

            out.println("}");
        }
    }

    private void generateHeaderClass(Path packageDir) {
        Path file = packageDir.resolve("Header.java");
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(file))) {
            out.print("""
                package %s;
                
                import fix.parser.message.base.Segment;
                import java.time.Instant;
                import java.time.LocalDate;
                
                public class Header {
                    private final Segment segment;
                
                    public Header(Segment segment) {
                        this.segment = segment;
                    }
                
                """.formatted(packageName));

            // Generate getters for header fields
            for (FieldDef field : spec.header().fields()) {
                generateFieldGetter(out, field);
            }

            // Generate getters for header groups
            for (GroupDef group : spec.header().groups()) {
                generateGroupGetter(out, group, packageDir);
            }

            out.println("}");
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Header class", e);
        }
    }

    private void generateTrailerClass(Path packageDir) {
        Path file = packageDir.resolve("Trailer.java");
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(file))) {
            out.print("""
                package %s;
                
                import fix.parser.message.base.Segment;
                import java.time.Instant;
                import java.time.LocalDate;
                
                public class Trailer {
                    private final Segment segment;
                
                    public Trailer(Segment segment) {
                        this.segment = segment;
                    }
                
                """.formatted(packageName));

            // Generate getters for trailer fields
            for (FieldDef field : spec.trailer().fields()) {
                generateFieldGetter(out, field);
            }

            out.println("}");
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Trailer class", e);
        }
    }

    private void generateComponentGetter(PrintWriter out, ComponentRef component) {
        String componentClassName = component.name() + "Component";

        out.printf("""
                public %s get%s() {
                    return new %s(this.segment);
                }
                
                """,
            componentClassName, component.name(),
            componentClassName);
    }
}
