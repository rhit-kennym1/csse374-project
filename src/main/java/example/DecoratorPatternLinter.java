package example;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

public class DecoratorPatternLinter implements PatternLinterInterface {

    private final ClassNode classNode;

    private boolean looksLikeDecorator = false;
    private String wrappedFieldName = "";
    private String wrappedFieldType = "";

    private boolean wrappedComponentIsPublic = false;
    private boolean noConstructorWithComponent = false;
    private boolean wrappedComponentNotFinal = false;
    private boolean doesNotImplementCommonInterface = false;

    public DecoratorPatternLinter(ClassNode classNode) {
        this.classNode = classNode;
    }

    public void lintClass() {
        checkForViolations();
        logError();
    }

    private void checkForViolations() {
        checkIfLooksLikeDecorator();

        if (looksLikeDecorator) {
            checkWrappedComponentEncapsulation();
            checkForConstructorWithComponent();
            checkForCommonInterface();
        }
    }

    private void checkIfLooksLikeDecorator() {
        boolean hasDecoratorInName = classNode.name.toLowerCase().contains("decorator") ||
                classNode.name.toLowerCase().contains("wrapper");

        if (!hasDecoratorInName) {
            boolean extendsDecorator = false;
            if (classNode.superName != null) {
                String superName = classNode.superName.toLowerCase();
                extendsDecorator = superName.contains("decorator") ||
                        superName.contains("wrapper") ||
                        superName.equals("java/io/filterinputstream") ||
                        superName.equals("java/io/filteroutputstream") ||
                        superName.equals("java/io/filterreader") ||
                        superName.equals("java/io/filterwriter");
            }

            if (!extendsDecorator) {
                return;
            }
        }

        boolean implementsInterface = classNode.interfaces != null && !classNode.interfaces.isEmpty();
        boolean extendsAbstractClass = classNode.superName != null && !classNode.superName.equals("java/lang/Object");

        if (!implementsInterface && !extendsAbstractClass) {
            return;
        }

        boolean hasWrappableField = false;

        for (FieldNode field : classNode.fields) {
            boolean isStatic = (field.access & Opcodes.ACC_STATIC) != 0;

            if (isStatic) {
                continue;
            }

            Type fieldType = Type.getType(field.desc);
            String fieldTypeInternal = fieldType.getInternalName();

            boolean fieldMatchesInterface = false;
            if (implementsInterface) {
                for (String iface : classNode.interfaces) {
                    if (fieldTypeInternal.equals(iface)) {
                        fieldMatchesInterface = true;
                        break;
                    }
                }
            }

            boolean fieldMatchesSuperclass = fieldTypeInternal.equals(classNode.superName);

            boolean isKnownDecoratorComponentType =
                    (classNode.superName != null && classNode.superName.contains("Filter")) &&
                            (fieldTypeInternal.contains("InputStream") ||
                                    fieldTypeInternal.contains("OutputStream") ||
                                    fieldTypeInternal.contains("Reader") ||
                                    fieldTypeInternal.contains("Writer"));

            if (fieldMatchesInterface || fieldMatchesSuperclass || isKnownDecoratorComponentType) {
                hasWrappableField = true;
                wrappedFieldName = field.name;
                wrappedFieldType = fieldTypeInternal;
                break;
            }
        }

        if (!hasWrappableField) {
            return;
        }

        looksLikeDecorator = true;
    }

    private void checkWrappedComponentEncapsulation() {
        for (FieldNode field : classNode.fields) {
            if (field.name.equals(wrappedFieldName)) {
                boolean isPublic = (field.access & Opcodes.ACC_PUBLIC) != 0;
                boolean isFinal = (field.access & Opcodes.ACC_FINAL) != 0;

                if (isPublic) {
                    wrappedComponentIsPublic = true;
                }

                if (!isFinal) {
                    wrappedComponentNotFinal = true;
                }

                break;
            }
        }

        for (MethodNode method : classNode.methods) {
            boolean isPublic = (method.access & Opcodes.ACC_PUBLIC) != 0;

            if (isPublic) {
                Type returnType = Type.getReturnType(method.desc);
                String returnTypeInternal = returnType.getInternalName();

                if (returnTypeInternal.equals(wrappedFieldType)) {
                    String methodNameLower = method.name.toLowerCase();

                    if (methodNameLower.startsWith("get") ||
                            methodNameLower.contains("component") ||
                            methodNameLower.contains("wrapped") ||
                            methodNameLower.equals(wrappedFieldName)) {
                        wrappedComponentIsPublic = true;
                    }
                }
            }
        }
    }

    private void checkForConstructorWithComponent() {
        boolean hasAppropriateConstructor = false;

        for (MethodNode method : classNode.methods) {
            if (method.name.equals("<init>")) {
                Type[] argTypes = Type.getArgumentTypes(method.desc);

                for (Type argType : argTypes) {
                    String argTypeInternal = argType.getInternalName();

                    if (argTypeInternal.equals(wrappedFieldType)) {
                        hasAppropriateConstructor = true;
                        break;
                    }

                    if (classNode.interfaces != null) {
                        for (String iface : classNode.interfaces) {
                            if (argTypeInternal.equals(iface)) {
                                hasAppropriateConstructor = true;
                                break;
                            }
                        }
                    }
                }

                if (hasAppropriateConstructor) {
                    break;
                }
            }
        }

        if (!hasAppropriateConstructor) {
            noConstructorWithComponent = true;
        }
    }

    private void checkForCommonInterface() {
        boolean hasCommonInterface = false;

        if (classNode.interfaces != null) {
            for (String iface : classNode.interfaces) {
                if (wrappedFieldType.equals(iface)) {
                    hasCommonInterface = true;
                    break;
                }
            }
        }

        if (wrappedFieldType.equals(classNode.superName)) {
            hasCommonInterface = true;
        }

        if (!hasCommonInterface) {
            doesNotImplementCommonInterface = true;
        }
    }

    private void logError() {
        if (!looksLikeDecorator) {
            System.out.println("No error in DecoratorPatternLinter for: " + classNode.name);
            return;
        }

        boolean hasViolations = wrappedComponentIsPublic ||
                noConstructorWithComponent ||
                wrappedComponentNotFinal ||
                doesNotImplementCommonInterface;

        if (hasViolations) {
            if (wrappedComponentIsPublic) {
                System.err.println(classNode.name + " exposes its wrapped component publicly!");
            }

            if (wrappedComponentNotFinal) {
                System.err.println(classNode.name + " has a wrapped component field that is not final!");
            }

            if (noConstructorWithComponent) {
                System.err.println(classNode.name + " lacks a constructor that accepts the component to wrap!");
            }

            if (doesNotImplementCommonInterface) {
                System.err.println(classNode.name + " does not share a common interface with its wrapped component!");
            }
        } else {
            System.out.println("No error in DecoratorPatternLinter for: " + classNode.name);
        }
    }
}