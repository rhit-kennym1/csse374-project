package example;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Analyzes entire packages for cyclic dependencies between classes
 */
public class PackageAnalyzer {

    private static final String CONFIG_PATH = "src/main/java/example/LinterConfig";
    private static final String PACKAGE_CONFIG_PATH = "src/main/java/example/PackageConfig";

    public static void main(String[] args) {
        System.out.println("=== Package-wide Cyclic Dependency Analysis ===\n");
        
        try (BufferedReader reader = new BufferedReader(new FileReader(PACKAGE_CONFIG_PATH))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                    continue;
                }
                analyzePackage(line.trim());
            }
        } catch (IOException e) {
            System.err.println("Error reading package config: " + e.getMessage());
            System.err.println("Make sure " + PACKAGE_CONFIG_PATH + " exists");
        }
    }

    private static void analyzePackage(String packagePath) {
        System.out.println("Analyzing package: " + packagePath);
        System.out.println("----------------------------------------");
        
        try {
            // Load all classes in the package
            Map<String, ClassNode> allClasses = loadPackageClasses(packagePath);
            
            if (allClasses.isEmpty()) {
                System.out.println("No classes found in package\n");
                return;
            }
            
            System.out.println("Found " + allClasses.size() + " classes");
            
            // Create a single cyclic dependency linter with all classes
            ClassNode firstClass = allClasses.values().iterator().next();
            CycleDependencyLinter linter = new CycleDependencyLinter(firstClass, allClasses);
            linter.lintClass();
            
            System.out.println();
            
        } catch (IOException e) {
            System.err.println("Error analyzing package: " + e.getMessage());
        }
    }

    private static Map<String, ClassNode> loadPackageClasses(String packagePath) throws IOException {
        Map<String, ClassNode> classes = new HashMap<>();
        
        // Convert package path to directory path
        String dirPath = "../main/resources/" + packagePath.replace('.', '/');
        Path packageDir = Paths.get(dirPath);
        
        // Check if directory exists
        if (!Files.exists(packageDir)) {
            // Try alternative path
            dirPath = "src/main/resources/" + packagePath.replace('.', '/');
            packageDir = Paths.get(dirPath);
        }
        
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

    private static ClassNode loadClassFromFile(Path classFile) throws IOException {
        byte[] classBytes = Files.readAllBytes(classFile);
        ClassReader classReader = new ClassReader(classBytes);
        ClassNode classNode = new ClassNode();
        classReader.accept(classNode, ClassReader.EXPAND_FRAMES);
        return classNode;
    }
}