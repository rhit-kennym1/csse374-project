package example;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

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
        String[] parts = line.split(":", 2);
        if (parts.length != 2) {
            System.err.println("Invalid config line: " + line);
            return;
        }

        String linterName = parts[0].trim();
        String targets = parts[1].trim();

        // Check if this is a package-wide analysis
        if (targets.startsWith("PACKAGE:")) {
            String packagePath = targets.substring("PACKAGE:".length()).trim();
            runPackageAnalysis(linterName, packagePath);
            return;
        }

        // GROUP_CLASSES:pkg:Class1,Class2,... only analyses the explicitly listed
        // classes
        if (targets.startsWith("GROUP_CLASSES:")) {
            String rest = targets.substring("GROUP_CLASSES:".length()).trim();
            int colon = rest.indexOf(':');
            if (colon < 0) {
                System.err.println("GROUP_CLASSES: requires format pkg:Class1,Class2,...");
                return;
            }
            String packagePath = rest.substring(0, colon).trim();
            String[] classNames = rest.substring(colon + 1).split(",");
            runGroupClasses(linterName, packagePath, classNames);
            return;
        }

        // Regular individual class analysis
        String[] classNames = targets.split(",");
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
            System.out.println();
            System.out.println("Running " + linter.getType() + " linter: " + linterName + " on " + className);
            linter.lintClass();

        } catch (IOException e) {
            System.err.println("Error loading class " + className + ": " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
        }
    }

    private static ClassNode loadClass(String className) throws IOException {
        // Build the file path directly
        String classPath = "src/test/resources/" + className.replace('.', '/') + ".class";

        try {
            // Read the file directly from filesystem
            Path filePath = Paths.get(classPath);
            byte[] classBytes = Files.readAllBytes(filePath);
            ClassReader classReader = new ClassReader(classBytes);

            ClassNode classNode = new ClassNode();
            classReader.accept(classNode, ClassReader.EXPAND_FRAMES);
            return classNode;
        } catch (Exception e) {
            // Fall back to classpath
            ClassReader classReader = new ClassReader(className);
            ClassNode classNode = new ClassNode();
            classReader.accept(classNode, ClassReader.EXPAND_FRAMES);
            return classNode;
        }
    }

    /**
     * Run package-wide analysis for linters that support it (like CyclicDependency)
     */
    private static void runPackageAnalysis(String linterName, String packagePath) {
        System.out.println("Running " + linterName + " package analysis on: " + packagePath);

        try {
            // Load all classes in the package
            Map<String, ClassNode> allClasses = loadPackageClasses(packagePath);

            if (allClasses.isEmpty()) {
                System.out.println("No classes found in package\n");
                return;
            }

            System.out.println("Found " + allClasses.size() + " classes");

            // Get first class as the base (needed for registry pattern)
            ClassNode firstClass = allClasses.values().iterator().next();

            // Try to create the linter using the registry
            try {
                Linter linter = LinterRegistry.createPackageLinter(linterName, firstClass, allClasses);
                linter.lintClass();
            } catch (IllegalArgumentException e) {
                System.err.println("Error: " + e.getMessage());
            }

            System.out.println();

        } catch (IOException e) {
            System.err.println("Error analyzing package: " + e.getMessage());
        }
    }

    private static void runGroupClasses(String linterName, String packagePath,
            String[] classNames) {
        try {
            Map<String, ClassNode> contextMap = loadPackageClasses(packagePath);
            if (contextMap.isEmpty()) {
                System.err.println("No classes found in package: " + packagePath);
                return;
            }

            for (String className : classNames) {
                className = className.trim();
                if (className.isEmpty())
                    continue;

                String internalName = className.replace('.', '/');
                ClassNode cn = contextMap.get(internalName);
                if (cn == null) {
                    System.err.println("Class not found in package context: " + className);
                    continue;
                }

                try {
                    Linter linter = LinterRegistry.createPackageLinter(linterName, cn, contextMap);
                    linter.lintClass();
                } catch (IllegalArgumentException e) {
                    System.err.println(e.getMessage());
                }
            }

        } catch (IOException e) {
            System.err.println("Error loading package context: " + e.getMessage());
        }
    }

    /**
     * Load all class files from a package directory
     */
    private static Map<String, ClassNode> loadPackageClasses(String packagePath) throws IOException {
        Map<String, ClassNode> classes = new HashMap<>();

        // Convert package path to directory path
        String dirPath = "src/test/resources/" + packagePath.replace('.', '/');
        Path packageDir = Paths.get(dirPath);

        if (!Files.exists(packageDir) || !Files.isDirectory(packageDir)) {
            System.err.println("Package directory not found: " + packageDir);
            return classes;
        }

        // Find all .class files in the directory
        try (Stream<Path> paths = Files.walk(packageDir, 1)) {
            paths.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".class"))
                    .forEach(classFile -> {
                        try {
                            ClassNode classNode = loadClassFromFile(classFile);
                            if (classNode != null) {
                                classes.put(classNode.name, classNode);
                            }
                        } catch (IOException e) {
                            System.err.println("Error loading " + classFile + ": " + e.getMessage());
                        }
                    });
        }

        return classes;
    }

    /**
     * Load a ClassNode from a file path
     */
    private static ClassNode loadClassFromFile(Path classFile) throws IOException {
        byte[] classBytes = Files.readAllBytes(classFile);
        ClassReader classReader = new ClassReader(classBytes);
        ClassNode classNode = new ClassNode();
        classReader.accept(classNode, ClassReader.EXPAND_FRAMES);
        return classNode;
    }
}