package fix.parser.codegen;

import java.nio.file.Path;
import java.nio.file.Paths;

public class GenerateFixClasses {
    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Usage: java Main <input-xml> <output-dir> <package-name>");
            System.exit(1);
        }

        try {
            Path inputFile = Paths.get(args[0]);
            Path outputDir = Paths.get(args[1]);
            String packageName = args[2];

            // Parse the XML specification
            FixSpecParser parser = new FixSpecParser(inputFile.toFile());
            FixSpec spec = parser.parse();

            // Generate code
            FixClassesGenerator generator = new FixClassesGenerator(spec, packageName, outputDir);
            generator.generate();

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}