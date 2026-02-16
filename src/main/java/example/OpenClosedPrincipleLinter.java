package example;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;

public class OpenClosedPrincipleLinter implements Linter {

    private final ClassNode classNode;
    private static final int PUBLIC_METHOD_THRESHOLD = 5;
    private static final double CONCRETE_RATIO_THRESHOLD = 2.0;

    public OpenClosedPrincipleLinter(ClassNode classNode) {
        this.classNode = classNode;
    }

    @Override
    public LinterType getType() {
        return LinterType.PRINCIPLE;
    }

    @Override
    public void lintClass() {
        List<String> violations = new ArrayList<>();

        OCPAnalysis analysis = analyzeClass();
        if (!analysis.publicMutableFields.isEmpty()) {
            violations.add(classNode.name + " has " + analysis.publicMutableFields.size() +
                    " public mutable field(s) - violates encapsulation (OCP). Fields: " +
                    String.join(", ", analysis.publicMutableFields));
        }
        if (analysis.shouldUseInterface) {
            violations.add(classNode.name + " is a concrete class with " +
                    analysis.publicMethodCount + " public methods but implements no interfaces - " +
                    "should define abstraction for extensibility (OCP)");
        }
        if (analysis.shouldBeFinal) {
            violations.add(classNode.name + " has overridable methods but is not designed for extension - " +
                    "should be marked final or provide proper extension points (OCP)");
        }
        if (analysis.tooMuchConcreteInAbstract) {
            violations.add(classNode.name + " is abstract but has " + analysis.concreteMethodCount +
                    " concrete methods vs " + analysis.abstractMethodCount +
                    " abstract methods - limits extensibility (OCP)");
        }

        if (violations.isEmpty()) {
            System.out.println("No error in OpenClosedPrincipleLinter for: " + classNode.name);
        } else {
            for (String violation : violations) {
                System.err.println(violation);
            }
        }
    }

    private OCPAnalysis analyzeClass() {
        OCPAnalysis analysis = new OCPAnalysis();

        boolean isFinal = (classNode.access & Opcodes.ACC_FINAL) != 0;
        boolean isAbstract = (classNode.access & Opcodes.ACC_ABSTRACT) != 0;
        boolean isInterface = (classNode.access & Opcodes.ACC_INTERFACE) != 0;
        boolean isEnum = (classNode.access & Opcodes.ACC_ENUM) != 0;

        if (isInterface || isEnum) {
            return analysis;
        }
        analyzeFields(analysis);
        analyzeMethods(analysis);
        if (!isFinal && !isAbstract) {
            boolean hasInterfaces = classNode.interfaces != null && !classNode.interfaces.isEmpty();

            if (analysis.publicMethodCount >= PUBLIC_METHOD_THRESHOLD && !hasInterfaces) {
                analysis.shouldUseInterface = true;
            }
            if (analysis.hasOverridableMethods && !hasInterfaces && !hasProtectedMethods(analysis)) {
                analysis.shouldBeFinal = true;
            }
        }
        if (isAbstract && !isFinal) {
            if (analysis.abstractMethodCount > 0 &&
                    analysis.concreteMethodCount > analysis.abstractMethodCount * CONCRETE_RATIO_THRESHOLD) {
                analysis.tooMuchConcreteInAbstract = true;
            }
        }

        return analysis;
    }

    private void analyzeFields(OCPAnalysis analysis) {
        for (FieldNode field : classNode.fields) {
            boolean isPublic = (field.access & Opcodes.ACC_PUBLIC) != 0;
            boolean isFinal = (field.access & Opcodes.ACC_FINAL) != 0;
            boolean isStatic = (field.access & Opcodes.ACC_STATIC) != 0;

            if (isPublic && !isFinal && !isStatic) {
                analysis.publicMutableFields.add(field.name);
            }
        }
    }

    private void analyzeMethods(OCPAnalysis analysis) {
        for (MethodNode method : classNode.methods) {
            boolean isPublic = (method.access & Opcodes.ACC_PUBLIC) != 0;
            boolean isProtected = (method.access & Opcodes.ACC_PROTECTED) != 0;
            boolean isPrivate = (method.access & Opcodes.ACC_PRIVATE) != 0;
            boolean isFinal = (method.access & Opcodes.ACC_FINAL) != 0;
            boolean isStatic = (method.access & Opcodes.ACC_STATIC) != 0;
            boolean isAbstract = (method.access & Opcodes.ACC_ABSTRACT) != 0;
            boolean isConstructor = method.name.equals("<init>");
            boolean isStaticInit = method.name.equals("<clinit>");

            if (isPublic && !isConstructor && !isStaticInit) {
                analysis.publicMethodCount++;
            }

            if ((isPublic || isProtected) && !isFinal && !isStatic && !isPrivate &&
                    !isConstructor && !isStaticInit) {
                analysis.hasOverridableMethods = true;

                if (isProtected) {
                    analysis.protectedMethodCount++;
                }
            }
            if (!isConstructor && !isStaticInit && !isStatic) {
                if (isAbstract) {
                    analysis.abstractMethodCount++;
                } else {
                    analysis.concreteMethodCount++;
                }
            }
        }
    }

    private boolean hasProtectedMethods(OCPAnalysis analysis) {
        return analysis.protectedMethodCount > 0;
    }

    private static class OCPAnalysis {
        List<String> publicMutableFields = new ArrayList<>();
        boolean shouldUseInterface = false;
        boolean shouldBeFinal = false;
        boolean tooMuchConcreteInAbstract = false;
        boolean hasOverridableMethods = false;

        int publicMethodCount = 0;
        int protectedMethodCount = 0;
        int abstractMethodCount = 0;
        int concreteMethodCount = 0;
    }
}