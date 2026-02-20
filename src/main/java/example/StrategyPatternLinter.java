package example;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.*;

public class StrategyPatternLinter implements Linter {

    private final ClassNode classNode;

    private static final int DELEGATION_BACKSCAN_LIMIT = 16;

    private static final int INJECTION_BACKSCAN_LIMIT = 12;

    public StrategyPatternLinter(ClassNode classNode) {
        this.classNode = classNode;
    }

    @Override
    public LinterType getType() {
        return LinterType.PATTERN;
    }

    @Override
    public void lintClass() {
        List<String> findings = new ArrayList<>();

        for (FieldNode field : classNode.fields) {
            if (!isCandidateField(field)) continue;

            Evidence ev = analyzeField(field);

            // "Strategy-like" only if it is injected AND delegated through
            if (ev.injectedFromOutside && !ev.delegations.isEmpty()) {
                findings.add(formatEvidence(ev));
            }
        }

        if (findings.isEmpty()) {
            System.out.println("No Strategy Pattern Detected for: " + classNode.name);
        } else {
            for (String s : findings) {
                System.out.println(s);
            }
        }
    }

    private boolean isCandidateField(FieldNode f) {
        if (f == null || f.desc == null) return false;
        if ((f.access & Opcodes.ACC_STATIC) != 0) return false;

        Type t = Type.getType(f.desc);
        if (t.getSort() != Type.OBJECT && t.getSort() != Type.ARRAY) return false;

        String internal = t.getInternalName();
        if (internal == null) return false;

        // Ignore common JDK types; strategies are usually project types
        if (internal.startsWith("java/")) return false;

        return true;
    }

    private Evidence analyzeField(FieldNode field) {
        Evidence ev = new Evidence();
        ev.field = field;
        ev.fieldTypeInternal = Type.getType(field.desc).getInternalName();

        // 1) Injection from outside: any method stores a param into this.field (PUTFIELD)
        ev.injectedFromOutside = detectInjectedFromOutside(field);

        // 2) Delegation: method calls that use this.field as receiver (GETFIELD ... INVOKE)
        ev.delegations = detectDelegations(field);

        return ev;
    }

    private boolean detectInjectedFromOutside(FieldNode field) {
        for (MethodNode m : classNode.methods) {
            if (m.instructions == null) continue;
            if (m.name.equals("<clinit>")) continue;

            Set<Integer> paramLocals = computeParamLocalIndexes(m);

            for (AbstractInsnNode insn = m.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (!(insn instanceof FieldInsnNode)) continue;

                FieldInsnNode fin = (FieldInsnNode) insn;
                if (fin.getOpcode() != Opcodes.PUTFIELD) continue;

                if (!fin.owner.equals(classNode.name)) continue;
                if (!fin.name.equals(field.name)) continue;
                if (!fin.desc.equals(field.desc)) continue;

                VarInsnNode loadedParam = findRecentParamLoad(paramLocals, insn, INJECTION_BACKSCAN_LIMIT);
                if (loadedParam != null) {
                    return true;
                }
            }
        }
        return false;
    }

    private Set<Integer> computeParamLocalIndexes(MethodNode m) {
        Set<Integer> paramLocals = new HashSet<>();
        Type[] args = Type.getArgumentTypes(m.desc);

        int idx = ((m.access & Opcodes.ACC_STATIC) != 0) ? 0 : 1;

        for (Type a : args) {
            paramLocals.add(idx);
            idx += a.getSize();
        }
        return paramLocals;
    }

    private VarInsnNode findRecentParamLoad(Set<Integer> paramLocals, AbstractInsnNode from, int maxSteps) {
        int steps = 0;
        AbstractInsnNode cur = from.getPrevious();

        while (cur != null && steps < maxSteps) {
            if (cur instanceof LineNumberNode || cur instanceof FrameNode || cur instanceof LabelNode) {
                cur = cur.getPrevious();
                continue;
            }

            if (cur instanceof VarInsnNode) {
                VarInsnNode vin = (VarInsnNode) cur;
                if (paramLocals.contains(vin.var) && isLoadOpcode(vin.getOpcode())) {
                    return vin;
                }
            }


            cur = cur.getPrevious();
            steps++;
        }

        return null;
    }

    private List<Delegation> detectDelegations(FieldNode field) {
        List<Delegation> out = new ArrayList<>();

        for (MethodNode m : classNode.methods) {
            if (m.instructions == null) continue;
            if (m.name.equals("<init>") || m.name.equals("<clinit>")) continue;

            for (AbstractInsnNode insn = m.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (!(insn instanceof MethodInsnNode)) continue;

                MethodInsnNode min = (MethodInsnNode) insn;

                FieldInsnNode backingGet = findRecentGetField(field, insn, DELEGATION_BACKSCAN_LIMIT);
                if (backingGet != null) {
                    int line = findLineNumberBefore(insn);
                    out.add(new Delegation(m.name, m.desc, min.owner, min.name, min.desc, line));
                }
            }
        }

        return out;
    }

    private FieldInsnNode findRecentGetField(FieldNode field, AbstractInsnNode from, int maxSteps) {
        int steps = 0;
        AbstractInsnNode cur = from.getPrevious();

        while (cur != null && steps < maxSteps) {
            if (cur instanceof LineNumberNode || cur instanceof FrameNode || cur instanceof LabelNode) {
                cur = cur.getPrevious();
                continue;
            }

            if (cur instanceof MethodInsnNode) {
                return null;
            }

            if (cur instanceof FieldInsnNode) {
                FieldInsnNode fin = (FieldInsnNode) cur;
                if (fin.getOpcode() == Opcodes.GETFIELD
                        && fin.owner.equals(classNode.name)
                        && fin.name.equals(field.name)
                        && fin.desc.equals(field.desc)) {
                    return fin;
                }
            }

            cur = cur.getPrevious();
            steps++;
        }

        return null;
    }

    private int findLineNumberBefore(AbstractInsnNode target) {
        AbstractInsnNode cur = target;
        while (cur != null) {
            if (cur instanceof LineNumberNode) return ((LineNumberNode) cur).line;
            cur = cur.getPrevious();
        }
        return -1;
    }

    private String formatEvidence(Evidence ev) {
        String type = (ev.fieldTypeInternal == null) ? ev.field.desc : ev.fieldTypeInternal.replace('/', '.');

        String lineInfo = "";
        if (!ev.delegations.isEmpty() && ev.delegations.get(0).line > 0) {
            lineInfo = " (line " + ev.delegations.get(0).line + ")";
        }

        StringBuilder targets = new StringBuilder();
        int limit = Math.min(3, ev.delegations.size());
        for (int i = 0; i < limit; i++) {
            Delegation d = ev.delegations.get(i);
            targets.append(d.calledOwner.replace('/', '.'))
                    .append(".")
                    .append(d.calledName)
                    .append(d.calledDesc);
            if (d.line > 0) targets.append("@").append(d.line);
            if (i < limit - 1) targets.append(", ");
        }
        if (ev.delegations.size() > limit) targets.append(" ...");

        return "[Strategy] Strategy pattern detected in " + classNode.name + lineInfo
                + " | field='" + ev.field.name + "'"
                + " | type=" + type
                + " | injectedFromOutside=" + ev.injectedFromOutside
                + " | delegations=" + ev.delegations.size()
                + " | examples: " + targets;
    }

    private boolean isLoadOpcode(int opcode) {
        return opcode == Opcodes.ILOAD
                || opcode == Opcodes.LLOAD
                || opcode == Opcodes.FLOAD
                || opcode == Opcodes.DLOAD
                || opcode == Opcodes.ALOAD;
    }

    private static class Evidence {
        FieldNode field;
        String fieldTypeInternal;
        boolean injectedFromOutside;
        List<Delegation> delegations = new ArrayList<>();
    }

    private static class Delegation {
        String inMethodName;
        String inMethodDesc;
        String calledOwner;
        String calledName;
        String calledDesc;
        int line;

        Delegation(String inMethodName, String inMethodDesc,
                   String calledOwner, String calledName, String calledDesc,
                   int line) {
            this.inMethodName = inMethodName;
            this.inMethodDesc = inMethodDesc;
            this.calledOwner = calledOwner;
            this.calledName = calledName;
            this.calledDesc = calledDesc;
            this.line = line;
        }
    }
}