package example;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class EqualsHashCodeLinter implements Linter {
    private final ClassNode classNode;
    private boolean hasEquals = false;
    private boolean hasHashCode = false;

    public EqualsHashCodeLinter(ClassNode classNode) {
        this.classNode = classNode;
    }

    @Override
    public LinterType getType() {
        return LinterType.CHECKSTYLE;
    }

    @Override
    public void lintClass() {
        checkForMethods();
        logError();
    }

    private void checkForMethods() {
        for (MethodNode methodNode : this.classNode.methods) {
            if (methodNode.name.equals("equals")) {
                hasEquals = true;
            }
            if (methodNode.name.equals("hashCode")) {
                hasHashCode = true;
            }
        }
    }

    private void logError() {
        boolean throwError = (hasEquals && !hasHashCode || !hasEquals && hasHashCode);
        if (throwError) {
            if (hasEquals) {
                System.err.println(classNode.name + " has equals method but no hashCode method!");
            } else {
                System.err.println(classNode.name + " has hashCode method but no equals method!");
            }
        } else {
            System.out.println("No error in EqualsHashCodeLinter for: " + classNode.name);
        }
    }
}