package example;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class ASMMain {
	/**
	 * Reads in a list of Java Classes and prints fun facts about them.
	 * For more information, read: https://asm.ow2.io/asm4-guide.pdf
	 *
	 * @param args
	 *            : the names of the classes, separated by spaces. For example:
	 *            java example.MyFirstLinter java.lang.String
	 *
	 */
	public static void main(String[] args) {
		String equalsHashCodeConfig = "src/main/java/example/EqualsHashCodeLinterConfig";
		runLinterFromConfig(equalsHashCodeConfig, LinterType.CHECKSTYLE, "EqualsHashCode");

		String ocpConfig = "src/main/java/example/OpenClosedPrincipleLinterConfig";
		runLinterFromConfig(ocpConfig, LinterType.PRINCIPLE, "OpenClosedPrinciple");
	}

	private enum LinterType {
		CHECKSTYLE,
		PRINCIPLE,
		PATTERN
	}

	private static void runLinterFromConfig(String configPath, LinterType linterType, String linterName) {
		try (BufferedReader reader = new BufferedReader(new FileReader(configPath))) {
			String line;
			while ((line = reader.readLine()) != null) {
				ClassReader classReader = new ClassReader(line);
				ClassNode classNode = new ClassNode();
				classReader.accept(classNode, ClassReader.EXPAND_FRAMES);
				Object linter = createLinter(linterType, linterName, classNode);
				if (linter == null) {
					System.err.println("Unknown linter: " + linterName);
					continue;
				}

				if (linter instanceof CheckstyleLinterInterface) {
					((CheckstyleLinterInterface) linter).lintClass();
				} else if (linter instanceof PrincipleLinterInterface) {
					((PrincipleLinterInterface) linter).lintClass();
				}
			}
		} catch (IOException e) {
			System.err.println("Error reading file " + configPath + ": " + e.getMessage());
		}
	}

	private static Object createLinter(LinterType type, String linterName, ClassNode classNode) {
		switch (type) {
			case CHECKSTYLE:
				return createCheckstyleLinter(linterName, classNode);
			case PRINCIPLE:
				return createPrincipleLinter(linterName, classNode);
			case PATTERN:
				return createPatternLinter(linterName, classNode);
			default:
				return null;
		}
	}

	private static CheckstyleLinterInterface createCheckstyleLinter(String name, ClassNode classNode) {
		switch (name) {
			case "EqualsHashCode":
				return new EqualsHashCodeLinter(classNode);
			default:
				return null;
		}
	}

	private static PrincipleLinterInterface createPrincipleLinter(String name, ClassNode classNode) {
		switch (name) {
			case "OpenClosedPrinciple":
				return new OpenClosedPrincipleLinter(classNode);
			default:
				return null;
		}
	}

	private static PatternLinterInterface createPatternLinter(String name, ClassNode classNode) {
		switch (name) {
			default:
				return null;
		}
	}
}