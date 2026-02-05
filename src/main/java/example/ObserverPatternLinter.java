package example;

import java.util.*;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class ObserverPatternLinter implements PatternLinterInterface {

    private ClassNode classNode;
    private List<String> warnings = new ArrayList<>();

    public ObserverPatternLinter(ClassNode classNode) {
        this.classNode = classNode;
    }

    @Override
    public void lintClass() {
        boolean hasObserverList = hasObserverCollection();
        boolean hasNotify = hasNotifyLoop();

        if (hasObserverList && hasNotify) {
            warnings.add("[Observer] Possible Subject: " + classNode.name);
        }

        warnings.forEach(System.out::println);
    }

    private boolean hasObserverCollection() {
        for (FieldNode f : classNode.fields) {
            if (f.desc.contains("List") || f.desc.contains("Set")) {
                return true;
            }
        }
        return false;
    }

    private boolean hasNotifyLoop() {
        for (MethodNode m : classNode.methods) {
            boolean loops = false;
            boolean callsUpdate = false;

            for (AbstractInsnNode insn : m.instructions) {

                if (insn.getOpcode() == Opcodes.INVOKEINTERFACE
                        || insn.getOpcode() == Opcodes.INVOKEVIRTUAL) {

                    MethodInsnNode call = (MethodInsnNode) insn;

                    if (call.name.toLowerCase().contains("update")
                            || call.name.toLowerCase().contains("notify")) {
                        callsUpdate = true;
                    }
                }

                if (insn.getOpcode() == Opcodes.GOTO
                        || insn.getOpcode() == Opcodes.IF_ICMPNE) {
                    loops = true;
                }
            }

            if (loops && callsUpdate)
                return true;
        }
        return false;
    }
}