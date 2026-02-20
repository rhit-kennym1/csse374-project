package example;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.*;


public class TemporalCouplingLinter implements Linter {

    private final ClassNode classNode;

    private static final Set<String> SETUP_NAMES = new HashSet<>(Arrays.asList(
            "init", "initialize", "open", "start", "begin", "connect", "load", "prepare", "configure", "setup"
    ));

    private static final Set<String> USE_NAMES = new HashSet<>(Arrays.asList(
            "execute", "run", "use", "send", "apply", "process", "commit", "save", "write", "read", "flush", "close"
    ));

    private static final Set<String> FLUENT_BUILDER_HINTS = new HashSet<>(Arrays.asList(
            "builder", "build", "with", "set"
    ));

    private static final int RECEIVER_BACKSCAN_LIMIT = 12;

    public TemporalCouplingLinter(ClassNode classNode) {
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
            if (m.instructions == null || m.instructions.size() == 0) continue;
            if (m.name.equals("<init>") || m.name.equals("<clinit>")) continue;

            findings.addAll(lintMethod(m));
        }

        if (findings.isEmpty()) {
            System.out.println("No Temporal Coupling Violation Detected for: " + classNode.name);
        } else {
            for (String s : findings) {
                System.err.println(s);
            }
        }
    }

    private List<String> lintMethod(MethodNode m) {
        List<String> out = new ArrayList<>();

        Map<Integer, Boolean> hasSetup = new HashMap<>();

        for (AbstractInsnNode insn = m.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (!(insn instanceof MethodInsnNode)) continue;

            MethodInsnNode min = (MethodInsnNode) insn;

            // Avoid java library noise
            if (min.owner == null || min.owner.startsWith("java/")) continue;

            String methodLower = min.name == null ? "" : min.name.toLowerCase();

            // Skip obvious fluent/builder chaining
            if (looksFluentOrBuilderish(methodLower)) continue;

            // Identify receiver local variable index
            Integer receiverVar = findReceiverLocalVar(insn, RECEIVER_BACKSCAN_LIMIT);
            if (receiverVar == null) continue;

            // Mark setup
            if (isSetupCall(methodLower)) {
                hasSetup.put(receiverVar, true);
                continue;
            }

            // Flag use-without-setup
            if (isUseCall(methodLower)) {
                boolean setupSeen = hasSetup.getOrDefault(receiverVar, false);
                if (!setupSeen) {
                    int line = findLineNumberBefore(insn);
                    String lineInfo = line > 0 ? " (line " + line + ")" : "";
                    out.add(classNode.name + "." + m.name + m.desc + lineInfo
                            + ": Temporal Coupling suspected - call to '"
                            + min.owner.replace('/', '.') + "." + min.name + min.desc
                            + "' on local var index " + receiverVar
                            + " without prior setup/init/open/start in this method.");
                }
            }
        }

        return out;
    }

    private boolean isSetupCall(String methodLower) {
        if (SETUP_NAMES.contains(methodLower)) return true;

        return methodLower.startsWith("init")
                || methodLower.startsWith("open")
                || methodLower.startsWith("start")
                || methodLower.startsWith("begin")
                || methodLower.startsWith("connect")
                || methodLower.startsWith("load")
                || methodLower.startsWith("prepare")
                || methodLower.startsWith("config");
    }

    private boolean isUseCall(String methodLower) {
        if (USE_NAMES.contains(methodLower)) return true;

        return methodLower.startsWith("exec")
                || methodLower.startsWith("run")
                || methodLower.startsWith("use")
                || methodLower.startsWith("send")
                || methodLower.startsWith("apply")
                || methodLower.startsWith("process")
                || methodLower.startsWith("commit")
                || methodLower.startsWith("save")
                || methodLower.startsWith("write")
                || methodLower.startsWith("read")
                || methodLower.startsWith("flush")
                || methodLower.startsWith("close");
    }

    private boolean looksFluentOrBuilderish(String methodLower) {
        if (methodLower.startsWith("set") || methodLower.startsWith("with")) return true;
        if (methodLower.equals("build") || methodLower.endsWith("builder")) return true;

        for (String hint : FLUENT_BUILDER_HINTS) {
            if (methodLower.contains(hint)) return true;
        }
        return false;
    }

    private Integer findReceiverLocalVar(AbstractInsnNode invokeInsn, int maxSteps) {
        int steps = 0;
        AbstractInsnNode cur = invokeInsn.getPrevious();

        while (cur != null && steps < maxSteps) {
            if (cur instanceof LineNumberNode || cur instanceof FrameNode || cur instanceof LabelNode) {
                cur = cur.getPrevious();
                continue;
            }

            if (cur instanceof MethodInsnNode) {
                return null;
            }

            if (cur instanceof VarInsnNode) {
                VarInsnNode vin = (VarInsnNode) cur;
                if (vin.getOpcode() == Opcodes.ALOAD) {
                    return vin.var;
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
}