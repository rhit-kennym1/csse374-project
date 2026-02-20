package example;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.List;

public class HollywoodPrincipleLinter implements Linter {

    private final ClassNode classNode;

    private static final int NEW_THRESHOLD = 5;
    private static final int GETTER_THRESHOLD = 8;
    private static final int CONDITION_THRESHOLD = 6;

    public HollywoodPrincipleLinter(ClassNode classNode) {
        this.classNode = classNode;
    }

    @Override
    public LinterType getType() {
        return LinterType.PRINCIPLE;
    }

    @Override
    public void lintClass() {
        List<String> findings = new ArrayList<>();

        for (MethodNode m : classNode.methods) {
            if (m.name.equals("<init>") || m.name.equals("<clinit>")) continue;
            if (m.instructions == null || m.instructions.size() == 0) continue;

            MethodScores scores = scoreMethod(m);

            boolean highNew = scores.newCount >= NEW_THRESHOLD;
            boolean highGetters = scores.getterCount >= GETTER_THRESHOLD;
            boolean highConditions = scores.conditionCount >= CONDITION_THRESHOLD;

            int highBuckets = (highNew ? 1 : 0) + (highGetters ? 1 : 0) + (highConditions ? 1 : 0);

            if (highBuckets >= 2) {
                int line = findFirstLineNumber(m);
                String lineInfo = line > 0 ? " (line " + line + ")" : "";

                findings.add(classNode.name + "." + m.name + m.desc + lineInfo
                        + ": Likely Hollywood Principle violation - "
                        + "NEW=" + scores.newCount
                        + ", dataPull(get*/is*/has*)=" + scores.getterCount
                        + ", conditions(if/switch)=" + scores.conditionCount
                        + ". Suggestion: push decisions into polymorphic collaborators / callbacks (IoC).");
            }
        }

        if (findings.isEmpty()) {
            System.out.println("No Hollywood Principle Violation Detected for: " + classNode.name);
        } else {
            for (String s : findings) {
                System.err.println(s);
            }
        }
    }

    private MethodScores scoreMethod(MethodNode m) {
        MethodScores scores = new MethodScores();

        for (AbstractInsnNode insn = m.instructions.getFirst(); insn != null; insn = insn.getNext()) {

            // Constructor call count: NEW instructions (exclude java/*)
            if (insn.getOpcode() == Opcodes.NEW && insn instanceof TypeInsnNode) {
                TypeInsnNode tin = (TypeInsnNode) insn;
                if (tin.desc != null && !tin.desc.startsWith("java/")) {
                    scores.newCount++;
                }
            }

            // Data pull count: get*/is*/has* method calls (exclude java/*, common collections)
            if (insn instanceof MethodInsnNode) {
                MethodInsnNode min = (MethodInsnNode) insn;
                if (min.owner != null && !min.owner.startsWith("java/")) {
                    String n = min.name;
                    if (n.startsWith("get") || n.startsWith("is") || n.startsWith("has")) {
                        scores.getterCount++;
                    }
                }
            }

            // Condition count: IF* + switch instructions
            int op = insn.getOpcode();
            if (op >= Opcodes.IFEQ && op <= Opcodes.IF_ACMPNE) {
                scores.conditionCount++;
            } else if (insn instanceof TableSwitchInsnNode || insn instanceof LookupSwitchInsnNode) {
                scores.conditionCount++;
            }
        }

        return scores;
    }

    private int findFirstLineNumber(MethodNode method) {
        if (method.instructions == null) return -1;
        for (AbstractInsnNode insn : method.instructions) {
            if (insn instanceof LineNumberNode) {
                return ((LineNumberNode) insn).line;
            }
        }
        return -1;
    }

    private static class MethodScores {
        int newCount = 0;
        int getterCount = 0;
        int conditionCount = 0;
    }
}
