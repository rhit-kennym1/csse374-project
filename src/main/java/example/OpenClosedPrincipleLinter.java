package example;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import java.util.List;

public class OpenClosedPrincipleLinter implements PrincipleLinterInterface {

    private final ClassNode classNode;
    private boolean shouldBeFinal = false;
    private boolean hasMutablePublicFields = false;
    private boolean lacksAbstraction = false;
    private boolean hasTooManyConcreteMethodsInAbstractClass = false;
    private int publicMethodCount = 0;
    private int abstractMethodCount = 0;
    private int concreteMethodCount = 0;

    public OpenClosedPrincipleLinter(ClassNode classNode) {
        this.classNode = classNode;
    }

    @Override
    public void lintClass() {
        checkForViolations();
        logError();
    }

    private void checkForViolations() {
        checkIfShouldBeFinal();
        checkForPublicMutableFields();
        checkForMissingAbstraction();
        checkForConcreteMethodsInExtensibleClass();
    }

    private void checkIfShouldBeFinal() {
        boolean isFinal = (classNode.access & Opcodes.ACC_FINAL) != 0;
        boolean isAbstract = (classNode.access & Opcodes.ACC_ABSTRACT) != 0;
        boolean isInterface = (classNode.access & Opcodes.ACC_INTERFACE) != 0;

        if (!isFinal && !isAbstract && !isInterface) {
            boolean hasOverridableMethods = hasOverridableMethods();

            if (hasOverridableMethods) {
                shouldBeFinal = true;
            }
        }
    }

    private void checkForPublicMutableFields() {
        List<FieldNode> fields = classNode.fields;

        for (FieldNode field : fields) {
            boolean isPublic = (field.access & Opcodes.ACC_PUBLIC) != 0;
            boolean isFinal = (field.access & Opcodes.ACC_FINAL) != 0;

            if (isPublic && !isFinal) {
                hasMutablePublicFields = true;
                break;
            }
        }
    }

    private void checkForMissingAbstraction() {
        boolean isAbstract = (classNode.access & Opcodes.ACC_ABSTRACT) != 0;
        boolean isInterface = (classNode.access & Opcodes.ACC_INTERFACE) != 0;
        boolean isFinal = (classNode.access & Opcodes.ACC_FINAL) != 0;

        if (isAbstract || isInterface || isFinal) {
            return;
        }

        boolean hasInterfaces = classNode.interfaces != null && !classNode.interfaces.isEmpty();

        publicMethodCount = 0;
        for (MethodNode method : classNode.methods) {
            boolean isPublic = (method.access & Opcodes.ACC_PUBLIC) != 0;
            boolean isConstructor = method.name.equals("<init>");

            if (isPublic && !isConstructor) {
                publicMethodCount++;
            }
        }

        if (publicMethodCount >= 5 && !hasInterfaces) {
            lacksAbstraction = true;
        }
    }

    private void checkForConcreteMethodsInExtensibleClass() {
        boolean isAbstract = (classNode.access & Opcodes.ACC_ABSTRACT) != 0;
        boolean isFinal = (classNode.access & Opcodes.ACC_FINAL) != 0;

        if (!isAbstract || isFinal) {
            return;
        }

        abstractMethodCount = 0;
        concreteMethodCount = 0;

        for (MethodNode method : classNode.methods) {
            boolean isMethodAbstract = (method.access & Opcodes.ACC_ABSTRACT) != 0;
            boolean isConstructor = method.name.equals("<init>");
            boolean isStatic = (method.access & Opcodes.ACC_STATIC) != 0;

            if (!isConstructor && !isStatic) {
                if (isMethodAbstract) {
                    abstractMethodCount++;
                } else {
                    concreteMethodCount++;
                }
            }
        }

        if (concreteMethodCount > abstractMethodCount * 3 && abstractMethodCount > 0) {
            hasTooManyConcreteMethodsInAbstractClass = true;
        }
    }

    private boolean hasOverridableMethods() {
        for (MethodNode method : classNode.methods) {
            boolean isPublic = (method.access & Opcodes.ACC_PUBLIC) != 0;
            boolean isProtected = (method.access & Opcodes.ACC_PROTECTED) != 0;
            boolean isFinal = (method.access & Opcodes.ACC_FINAL) != 0;
            boolean isStatic = (method.access & Opcodes.ACC_STATIC) != 0;
            boolean isPrivate = (method.access & Opcodes.ACC_PRIVATE) != 0;
            boolean isConstructor = method.name.equals("<init>");

            if ((isPublic || isProtected) && !isFinal && !isStatic && !isPrivate && !isConstructor) {
                return true;
            }
        }
        return false;
    }

    private void logError() {
        boolean hasViolations = shouldBeFinal || hasMutablePublicFields ||
                lacksAbstraction || hasTooManyConcreteMethodsInAbstractClass;

        if (hasViolations) {
            if (shouldBeFinal) {
                System.err.println(classNode.name + " is not final but has overridable methods!");
            }

            if (hasMutablePublicFields) {
                System.err.println(classNode.name + " has public mutable fields that violate OCP!");
            }

            if (lacksAbstraction) {
                System.err.println(classNode.name + " has " + publicMethodCount +
                        " public methods but no interface/abstraction!");
            }

            if (hasTooManyConcreteMethodsInAbstractClass) {
                System.err.println(classNode.name + " is abstract but has too many concrete methods (" +
                        concreteMethodCount + ") vs abstract methods (" + abstractMethodCount + ")!");
            }
        } else {
            System.out.println("No error in OpenClosedPrincipleLinter for: " + classNode.name);
        }
    }
}