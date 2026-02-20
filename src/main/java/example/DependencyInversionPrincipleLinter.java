package example;

import java.util.*;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class DependencyInversionPrincipleLinter implements Linter {

    private final ClassNode classNode;
    private final Map<String, ClassNode> classMap;

    public DependencyInversionPrincipleLinter(ClassNode classNode, Map<String, ClassNode> classMap) {
        this.classNode = classNode;
        this.classMap = classMap != null ? classMap : new HashMap<>();
    }

    public DependencyInversionPrincipleLinter(ClassNode classNode) {
        this(classNode, new HashMap<>());
    }

    @Override
    public LinterType getType() {
        return LinterType.PRINCIPLE;
    }

    @Override
    public void lintClass() {
        if ((classNode.access & Opcodes.ACC_INTERFACE) != 0
                || (classNode.access & Opcodes.ACC_ABSTRACT) != 0) {
            return;
        }

        List<String> warnings = new ArrayList<>();
        checkFieldTypes(classNode, warnings);
        checkMethodParameters(classNode, warnings);
        checkDirectInstantiations(classNode, warnings);

        warnings.forEach(System.out::println);
    }

    private void checkFieldTypes(ClassNode cn, List<String> warnings) {
        for (FieldNode f : cn.fields) {
            if ((f.access & Opcodes.ACC_STATIC) != 0)
                continue;

            String typeName = objectTypeFromDesc(f.desc);
            if (typeName == null || isExcluded(cn, typeName))
                continue;

            ClassNode typeNode = classMap.get(typeName);
            if (typeNode != null && isConcrete(typeNode)) {
                warnings.add("[DIP] Field '" + f.name + "' in " + cn.name
                        + " depends on concrete type: " + typeName);
            }
        }
    }

    private void checkMethodParameters(ClassNode cn, List<String> warnings) {
        for (MethodNode m : cn.methods) {
            if (m.name.equals("<init>") || m.name.equals("<clinit>"))
                continue;

            for (Type arg : Type.getArgumentTypes(m.desc)) {
                if (arg.getSort() != Type.OBJECT)
                    continue;

                String typeName = arg.getInternalName();
                if (isExcluded(cn, typeName))
                    continue;

                ClassNode typeNode = classMap.get(typeName);
                if (typeNode != null && isConcrete(typeNode)) {
                    warnings.add("[DIP] Method '" + m.name + "' in " + cn.name
                            + " has concrete parameter type: " + typeName);
                }
            }
        }
    }

    private void checkDirectInstantiations(ClassNode cn, List<String> warnings) {
        for (MethodNode m : cn.methods) {
            for (AbstractInsnNode insn : m.instructions) {
                if (insn.getOpcode() != Opcodes.INVOKESPECIAL)
                    continue;

                MethodInsnNode call = (MethodInsnNode) insn;
                if (!call.name.equals("<init>"))
                    continue;
                if (call.owner.equals(cn.name))
                    continue;
                if (isExcluded(cn, call.owner))
                    continue;

                ClassNode targetNode = classMap.get(call.owner);
                if (targetNode != null && isConcrete(targetNode)) {
                    warnings.add("[DIP] " + cn.name
                            + " directly instantiates concrete type: " + call.owner);
                }
            }
        }
    }

    private String objectTypeFromDesc(String desc) {
        if (desc.startsWith("L") && desc.endsWith(";")) {
            return desc.substring(1, desc.length() - 1);
        }
        return null;
    }

    private boolean isConcrete(ClassNode cn) {
        return (cn.access & Opcodes.ACC_INTERFACE) == 0
                && (cn.access & Opcodes.ACC_ABSTRACT) == 0;
    }

    private boolean isExcluded(ClassNode cn, String internalName) {
        return internalName.startsWith("java/")
                || internalName.startsWith("javax/")
                || internalName.startsWith("sun/")
                || internalName.equals(cn.name);
    }
}
