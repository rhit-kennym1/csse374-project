package example;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.*;

public class UnusedVariablesLinter implements Linter {

    private final ClassNode classNode;
//    private Set<String> UnusedVariable;

    public UnusedVariablesLinter(ClassNode classNode) {
        this.classNode = classNode;
    }

    @Override
    public void lintClass() {
        List<String> violations = new ArrayList<>();

        violations.addAll(findUnusedPrivateFields());
        violations.addAll(findUnusedPrivateMethods());
        violations.addAll(findUnusedLocals());

        if (violations.isEmpty()) {
            System.out.println("No error in UnusedVariablesLinter for: " + classNode.name);
        } else {
            for (String v : violations) {
                System.err.println(v);
            }
        }
    }

    // ----------------------------
    // Unused PRIVATE fields
    // ----------------------------
    private List<String> findUnusedPrivateFields() {
        List<String> out = new ArrayList<>();

        Map<String, FieldNode> privateFields = new HashMap<>();
        for (FieldNode f : classNode.fields) {
            if (isPrivate(f.access) && !isSynthetic(f.access)) {
                privateFields.put(fieldKey(classNode.name, f.name, f.desc), f);
            }
        }
        if (privateFields.isEmpty()) return out;

        Set<String> usedFields = new HashSet<>();
        for (MethodNode m : classNode.methods) {
            if (m.instructions == null) continue;
            for (AbstractInsnNode insn = m.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof FieldInsnNode) {
                    FieldInsnNode fin = (FieldInsnNode) insn;
                    String key = fieldKey(fin.owner, fin.name, fin.desc);
                    if (privateFields.containsKey(key)) {
                        usedFields.add(key);
                    }
                }
            }
        }

        for (Map.Entry<String, FieldNode> e : privateFields.entrySet()) {
            if (!usedFields.contains(e.getKey())) {
                FieldNode f = e.getValue();
                out.add(classNode.name + " has unused private field: " + f.name + ". Description: " + f.desc);
            }
        }
        return out;
    }

    // ----------------------------
    // Unused PRIVATE methods
    // ----------------------------
    private List<String> findUnusedPrivateMethods() {
        List<String> out = new ArrayList<>();

        Map<String, MethodNode> privateMethods = new HashMap<>();
        for (MethodNode m : classNode.methods) {
            if (!isPrivate(m.access)) continue;
            if (isSynthetic(m.access) || isBridge(m.access)) continue;
            if (m.name.equals("<init>") || m.name.equals("<clinit>")) continue;
            privateMethods.put(methodKey(classNode.name, m.name, m.desc), m);
        }
        if (privateMethods.isEmpty()) return out;

        Set<String> called = new HashSet<>();
        for (MethodNode m : classNode.methods) {
            if (m.instructions == null) continue;
            for (AbstractInsnNode insn = m.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof MethodInsnNode) {
                    MethodInsnNode min = (MethodInsnNode) insn;
                    if (min.owner != null && min.owner.equals(classNode.name)) {
                        called.add(methodKey(min.owner, min.name, min.desc));
                    }
                }
            }
        }

        for (Map.Entry<String, MethodNode> e : privateMethods.entrySet()) {
            if (!called.contains(e.getKey())) {
                MethodNode m = e.getValue();
                out.add(classNode.name + " has unused private method: " + m.name + ". Description: " + m.desc);
            }
        }
        return out;
    }

    // ----------------------------
    // Unused locals (stored but never loaded)
    // ----------------------------
    private List<String> findUnusedLocals() {
        List<String> out = new ArrayList<>();

        for (MethodNode m : classNode.methods) {
            if (m.instructions == null || m.instructions.size() == 0) continue;

            Map<Integer, int[]> counts = new HashMap<>();

            for (AbstractInsnNode insn = m.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (!(insn instanceof VarInsnNode)) continue;

                VarInsnNode vin = (VarInsnNode) insn;
                int idx = vin.var;

                if (!isStatic(m.access) && idx == 0) continue;

                int op = vin.getOpcode();
                if (isStoreOpcode(op)) {
                    counts.computeIfAbsent(idx, k -> new int[2])[0]++; // stores
                } else if (isLoadOpcode(op)) {
                    counts.computeIfAbsent(idx, k -> new int[2])[1]++; // loads
                }
            }

            Map<Integer, String> localNames = new HashMap<>();
            if (m.localVariables != null) {
                for (LocalVariableNode lvn : m.localVariables) {
                    if (lvn != null) {
                        localNames.put(lvn.index, lvn.name);
                    }
                }
            }

            for (Map.Entry<Integer, int[]> e : counts.entrySet()) {
                int idx = e.getKey();
                int stores = e.getValue()[0];
                int loads = e.getValue()[1];

                if (stores > 0 && loads == 0) {
                    String name = localNames.getOrDefault(idx, "var" + idx);
                    out.add(classNode.name + " :: " + m.name + m.desc
                            + " has unused local: " + name + " (index " + idx + ")");
                }
            }
        }

        return out;
    }

    
    // ----------------------------
    //           HELPERS
    // ---------------------------- 
    private static boolean isLoadOpcode(int op) {
        return op == Opcodes.ILOAD
                || op == Opcodes.LLOAD
                || op == Opcodes.FLOAD
                || op == Opcodes.DLOAD
                || op == Opcodes.ALOAD;
    }

    private static boolean isStoreOpcode(int op) {
        return op == Opcodes.ISTORE
                || op == Opcodes.LSTORE
                || op == Opcodes.FSTORE
                || op == Opcodes.DSTORE
                || op == Opcodes.ASTORE;
    }

    private static boolean isPrivate(int access) {
        return (access & Opcodes.ACC_PRIVATE) != 0;
    }

    private static boolean isStatic(int access) {
        return (access & Opcodes.ACC_STATIC) != 0;
    }

    private static boolean isSynthetic(int access) {
        return (access & Opcodes.ACC_SYNTHETIC) != 0;
    }

    private static boolean isBridge(int access) {
        return (access & Opcodes.ACC_BRIDGE) != 0;
    }

    private static String fieldKey(String owner, String name, String desc) {
        return owner + "#" + name + ":" + desc;
    }

    private static String methodKey(String owner, String name, String desc) {
        return owner + "#" + name + desc;
    }

	@Override
	public LinterType getType() {
		return LinterType.CHECKSTYLE;
	}
}