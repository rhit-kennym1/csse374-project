package example;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;

public class DecoratorPatternLinter implements Linter {

    private final ClassNode classNode;

    public DecoratorPatternLinter(ClassNode classNode) {
        this.classNode = classNode;
    }

    @Override
    public LinterType getType() {
        return LinterType.PATTERN;
    }

    @Override
    public void lintClass() {
        List<String> violations = new ArrayList<>();
        DecoratorAnalysis analysis = analyzeForDecoratorPattern();
        if (!analysis.looksLikeDecorator) {
            System.out.println("No error in DecoratorPatternLinter for: " + classNode.name);
            return;
        }
        if (analysis.wrappedField == null) {
            violations.add(classNode.name + " appears to be a decorator but has no wrapped component field!");
        } else {
            if (analysis.wrappedComponentIsPublic) {
                violations.add(classNode.name + " exposes its wrapped component publicly (field: " +
                        analysis.wrappedField.name + ")");
            }
            if (!analysis.wrappedComponentIsFinal) {
                violations.add(classNode.name + " has a wrapped component field that is not final (field: " +
                        analysis.wrappedField.name + ")");
            }
            if (!analysis.hasConstructorWithComponent) {
                violations.add(classNode.name + " lacks a constructor that accepts the component to wrap!");
            }
            if (!analysis.sharesInterfaceWithComponent) {
                violations.add(classNode.name + " does not implement the same interface/superclass as its wrapped component!");
            }
        }
        if (violations.isEmpty()) {
            System.out.println("No error in DecoratorPatternLinter for: " + classNode.name);
        } else {
            for (String violation : violations) {
                System.err.println(violation);
            }
        }
    }

    private DecoratorAnalysis analyzeForDecoratorPattern() {
        DecoratorAnalysis analysis = new DecoratorAnalysis();
        boolean hasDecoratorInName = classNode.name.toLowerCase().contains("decorator") ||
                classNode.name.toLowerCase().contains("wrapper");
        boolean extendsDecoratorClass = false;
        if (classNode.superName != null) {
            String superName = classNode.superName.toLowerCase();
            extendsDecoratorClass = superName.contains("decorator") ||
                    superName.contains("wrapper") ||
                    superName.equals("java/io/filterinputstream") ||
                    superName.equals("java/io/filteroutputstream") ||
                    superName.equals("java/io/filterreader") ||
                    superName.equals("java/io/filterwriter");
        }
        if (!hasDecoratorInName && !extendsDecoratorClass) {
            return analysis;
        }
        boolean implementsInterface = classNode.interfaces != null && !classNode.interfaces.isEmpty();
        boolean extendsNonObject = classNode.superName != null &&
                !classNode.superName.equals("java/lang/Object");

        if (!implementsInterface && !extendsNonObject) {
            return analysis;
        }

        FieldNode wrappedField = findWrappedComponentField();

        if (wrappedField == null) {
            analysis.looksLikeDecorator = true;
            return analysis;
        }

        analysis.looksLikeDecorator = true;
        analysis.wrappedField = wrappedField;
        analysis.wrappedComponentIsPublic = (wrappedField.access & Opcodes.ACC_PUBLIC) != 0 ||
                hasPublicGetterForField(wrappedField);
        analysis.wrappedComponentIsFinal = (wrappedField.access & Opcodes.ACC_FINAL) != 0;
        analysis.hasConstructorWithComponent = hasConstructorAcceptingComponent(wrappedField);
        analysis.sharesInterfaceWithComponent = sharesInterfaceWithComponent(wrappedField);
        return analysis;
    }

    private FieldNode findWrappedComponentField() {
        for (FieldNode field : classNode.fields) {
            if ((field.access & Opcodes.ACC_STATIC) != 0) {
                continue;
            }
            Type fieldType = Type.getType(field.desc);
            if (fieldType.getSort() != Type.OBJECT && fieldType.getSort() != Type.ARRAY) {
                continue;
            }
            String fieldTypeInternal = fieldType.getInternalName();
            if (classNode.interfaces != null) {
                for (String iface : classNode.interfaces) {
                    if (fieldTypeInternal.equals(iface)) {
                        return field;
                    }
                }
            }
            if (classNode.superName != null &&
                    !classNode.superName.equals("java/lang/Object") &&
                    fieldTypeInternal.equals(classNode.superName)) {
                return field;
            }
            if (classNode.superName != null && classNode.superName.contains("Filter")) {
                if (fieldTypeInternal.contains("InputStream") ||
                        fieldTypeInternal.contains("OutputStream") ||
                        fieldTypeInternal.contains("Reader") ||
                        fieldTypeInternal.contains("Writer")) {
                    return field;
                }
            }
        }

        return null;
    }

    private boolean hasPublicGetterForField(FieldNode field) {
        Type fieldType = Type.getType(field.desc);
        String fieldTypeDesc = fieldType.getDescriptor();

        for (MethodNode method : classNode.methods) {
            if ((method.access & Opcodes.ACC_PUBLIC) == 0) {
                continue;
            }
            Type returnType = Type.getReturnType(method.desc);
            if (!returnType.getDescriptor().equals(fieldTypeDesc)) {
                continue;
            }
            String methodNameLower = method.name.toLowerCase();
            String fieldNameLower = field.name.toLowerCase();

            if (methodNameLower.equals("get" + fieldNameLower) ||
                    methodNameLower.equals(fieldNameLower) ||
                    methodNameLower.contains("component") ||
                    methodNameLower.contains("wrapped") ||
                    methodNameLower.contains("delegate")) {
                return true;
            }
        }

        return false;
    }

    private boolean hasConstructorAcceptingComponent(FieldNode wrappedField) {
        Type fieldType = Type.getType(wrappedField.desc);
        String fieldTypeInternal = fieldType.getInternalName();

        for (MethodNode method : classNode.methods) {
            if (!method.name.equals("<init>")) {
                continue;
            }

            Type[] argTypes = Type.getArgumentTypes(method.desc);

            for (Type argType : argTypes) {
                if (argType.getSort() == Type.OBJECT || argType.getSort() == Type.ARRAY) {
                    String argTypeInternal = argType.getInternalName();

                    if (argTypeInternal.equals(fieldTypeInternal)) {
                        return true;
                    }
                    if (classNode.interfaces != null) {
                        for (String iface : classNode.interfaces) {
                            if (argTypeInternal.equals(iface)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    private boolean sharesInterfaceWithComponent(FieldNode wrappedField) {
        Type fieldType = Type.getType(wrappedField.desc);
        String fieldTypeInternal = fieldType.getInternalName();
        if (classNode.interfaces != null) {
            for (String iface : classNode.interfaces) {
                if (fieldTypeInternal.equals(iface)) {
                    return true;
                }
            }
        }
        if (classNode.superName != null &&
                !classNode.superName.equals("java/lang/Object") &&
                fieldTypeInternal.equals(classNode.superName)) {
            return true;
        }

        return false;
    }

    private static class DecoratorAnalysis {
        boolean looksLikeDecorator = false;
        FieldNode wrappedField = null;
        boolean wrappedComponentIsPublic = false;
        boolean wrappedComponentIsFinal = false;
        boolean hasConstructorWithComponent = false;
        boolean sharesInterfaceWithComponent = false;
    }
}