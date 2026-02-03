package example;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

// FIXME: this code has TERRIBLE DESIGN all around
public class MyFirstLinter {
	
	String[] fieldForAnalysisByThisProgram = new String[1];
	
	/**
	 * Reads in a list of Java Classes and prints fun facts about them.
	 * 
	 * For more information, read: https://asm.ow2.io/asm4-guide.pdf
	 * 
	 * @param args
	 *            : the names of the classes, separated by spaces. For example:
	 *            java example.MyFirstLinter java.lang.String
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public static void main(String[] args) throws IOException {
		String equalsHashCodeConfig = "src/main/java/example/EqualsHashCodeLinterConfig";
		try (BufferedReader reader = new BufferedReader(new FileReader(equalsHashCodeConfig))) {
			String line;
			while ((line = reader.readLine()) != null) {
				ClassReader classReader = new ClassReader(line);
				ClassNode classNode = new ClassNode();
				classReader.accept(classNode, ClassReader.EXPAND_FRAMES);
				EqualsHashCodeLinter ehcLinter = new EqualsHashCodeLinter(classNode);
				ehcLinter.lintClass();
			}
		} catch (IOException e) {
			System.err.println("Error reading file: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
