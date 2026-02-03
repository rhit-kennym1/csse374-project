package example;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class EqualsHashCodeLinter {

    private final ClassNode classNode;
    private boolean hasEquals = false;
    private boolean hasHashCode = false;

    public EqualsHashCodeLinter(ClassNode classNode) {
        this.classNode = classNode;
    }
    public void lintClass() {
        for (MethodNode methodNode : this.classNode.methods) {
            if (methodNode.name.equals("equals")) {
                hasEquals = true;
            }
            if (methodNode.name.equals("hashCode")) {
                hasHashCode = true;
            }
        }
        boolean throwError = (hasEquals && !hasHashCode || !hasEquals && hasHashCode);
        if (throwError) {
            System.err.println("Error in EqualsHashCodeLinter for: " + classNode.name);
        }else{
            System.out.println("No error in EqualsHashCodeLinter for: " + classNode.name);
        }
    }
}
