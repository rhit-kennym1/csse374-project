package example;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;

public class ASMMain {

    private static final String CONFIG_PATH = "src/main/java/example/LinterConfig";

    public static void main(String[] args) {
        try (BufferedReader reader = new BufferedReader(new FileReader(CONFIG_PATH))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty())
                    continue;
                if (line.trim().startsWith("#"))
                    continue;
                processConfigLine(line);
            }
        } catch (IOException e) {
            System.err.println("Error reading config: " + e.getMessage());
        }
    }

    private static void processConfigLine(String line) {
        String[] parts = line.split(":");
        if (parts.length != 2) {
            System.err.println("Invalid config line: " + line);
            return;
        }

        String linterName = parts[0].trim();
        String[] classNames = parts[1].split(",");

        for (String className : classNames) {
            className = className.trim();
            if (className.isEmpty())
                continue;
            runLinter(linterName, className);
        }
    }

    private static void runLinter(String linterName, String className) {
        try {
            ClassNode classNode = loadClass(className);

            Linter linter = LinterRegistry.create(linterName, classNode);
            System.out.println("Running " + linter.getType() + " linter: " + linterName + " on " + className);
            linter.lintClass();

        } catch (IOException e) {
            System.err.println("Error loading class " + className + ": " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
        }
    }

    private static ClassNode loadClass(String className) throws IOException {
        // Try loading from resources first
        String resourcePath = "../test/resources/testclasses/" + className.replace('.', '/') + ".class";
        InputStream is = ASMMain.class.getResourceAsStream(resourcePath);

        ClassReader classReader;
        if (is != null) {
            // Found in resources
            classReader = new ClassReader(is);
            is.close();
        } else {
            // Fall back to classpath
            classReader = new ClassReader(className);
        }

        ClassNode classNode = new ClassNode();
        classReader.accept(classNode, ClassReader.EXPAND_FRAMES);
        return classNode;
    }
}