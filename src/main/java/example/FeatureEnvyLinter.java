package example;

import org.objectweb.asm.tree.*;
import java.util.HashMap;
import java.util.Map;

public class FeatureEnvyLinter implements Linter {
    private final ClassNode classNode;
    private static final double ENVY_THRESHOLD = 0.6;

    public FeatureEnvyLinter(ClassNode classNode) {
        this.classNode = classNode;
    }

    @Override
    public LinterType getType() {
        return LinterType.CHECKSTYLE;
    }

    @Override
    public void lintClass() {
        for (MethodNode method : classNode.methods) {
            checkMethodForFeatureEnvy(method);
        }
    }

    private void checkMethodForFeatureEnvy(MethodNode method) {
        if (method.name.equals("<init>") || method.name.equals("<clinit>")) {
            return;
        }

        Map<String, Integer> accessCounts = new HashMap<>();
        accessCounts.put(classNode.name, 0);

        if (method.instructions == null) {
            return;
        }

        for (AbstractInsnNode insn : method.instructions) {
            if (insn instanceof FieldInsnNode) {
                FieldInsnNode fieldInsn = (FieldInsnNode) insn;
                String owner = fieldInsn.owner;

                if (!owner.equals(classNode.name) && !owner.startsWith("java/")) {
                    accessCounts.put(owner, accessCounts.getOrDefault(owner, 0) + 1);
                }
            } else if (insn instanceof MethodInsnNode) {
                MethodInsnNode methodInsn = (MethodInsnNode) insn;
                String owner = methodInsn.owner;

                if (shouldCountMethodAccess(methodInsn)) {
                    accessCounts.put(owner, accessCounts.getOrDefault(owner, 0) + 1);
                }
            }
        }

        int totalAccesses = accessCounts.values().stream().mapToInt(Integer::intValue).sum();

        if (totalAccesses == 0) {
            return;
        }

        Map<String, Integer> foreignAccesses = new HashMap<>();
        for (Map.Entry<String, Integer> entry : accessCounts.entrySet()) {
            if (!entry.getKey().equals(classNode.name)) {
                foreignAccesses.put(entry.getKey(), entry.getValue());
            }
        }

        for (Map.Entry<String, Integer> entry : foreignAccesses.entrySet()) {
            String foreignClass = entry.getKey();
            int foreignCount = entry.getValue();
            double envyRatio = (double) foreignCount / totalAccesses;

            if (envyRatio >= ENVY_THRESHOLD) {
                int line = findLineNumber(method);
                String lineInfo = line > 0 ? " (line " + line + ")" : "";
                String readableForeignClass = foreignClass.replace('/', '.');

                System.err.println(classNode.name + "." + method.name + method.desc
                        + lineInfo + ": Feature Envy detected - "
                        + String.format("%.0f%%", envyRatio * 100)
                        + " of accesses are to " + readableForeignClass
                        + " (" + foreignCount + "/" + totalAccesses + " accesses)");
            }
        }
    }

    private boolean shouldCountMethodAccess(MethodInsnNode methodInsn) {
        if (methodInsn.owner.startsWith("java/lang/")) {
            return false;
        }

        if (methodInsn.owner.startsWith("java/util/")) {
            return false;
        }

        return !methodInsn.owner.startsWith("java/io/");
    }

    private int findLineNumber(MethodNode method) {
        if (method.instructions == null) {
            return -1;
        }

        for (AbstractInsnNode insn : method.instructions) {
            if (insn instanceof LineNumberNode) {
                return ((LineNumberNode) insn).line;
            }
        }
        return -1;
    }
}