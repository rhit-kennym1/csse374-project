package example;

import java.util.*;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class ObserverPatternLinter implements Linter {

    private final ClassNode classNode;
    private final Map<String, ClassNode> classMap;
    private final List<String> warnings = new ArrayList<>();

    public ObserverPatternLinter(ClassNode classNode, Map<String, ClassNode> classMap) {
        this.classNode = classNode;
        this.classMap = classMap != null ? classMap : new HashMap<>();
    }

    public ObserverPatternLinter(ClassNode classNode) {
        this(classNode, new HashMap<>());
    }

    @Override
    public LinterType getType() {
        return LinterType.PATTERN;
    }

    @Override
    public void lintClass() {
        if (hasObserverCollection() && hasNotifyCall()) {
            warnings.add("[Observer] Subject detected: " + classNode.name);
            if (!hasRemoveObserverMethod()) {
                warnings.add("[Observer] Warning - no remove/unsubscribe method found" + classNode.name);
            }
        }

        if (isObserverImplementation()) {
            warnings.add("[Observer] Observer detected: " + classNode.name);
        }

        if (warnings.isEmpty()) {
            System.out.println("No error in ObserverPatternLinter for: " + classNode.name);
        } else {
            warnings.forEach(System.out::println);
        }
    }

    private boolean hasObserverCollection() {
        for (FieldNode f : classNode.fields) {
            if (f.desc.contains("List") || f.desc.contains("Set")
                    || f.desc.contains("Collection") || f.desc.contains("Deque")
                    || f.desc.contains("Queue") || f.desc.startsWith("[L")) {
                return true;
            }
        }
        return false;
    }

    private boolean hasNotifyCall() {
        for (MethodNode m : classNode.methods) {
            for (AbstractInsnNode insn : m.instructions) {
                if (insn.getOpcode() == Opcodes.INVOKEINTERFACE
                        || insn.getOpcode() == Opcodes.INVOKEVIRTUAL) {
                    MethodInsnNode call = (MethodInsnNode) insn;
                    if (isNotifyMethodName(call.name))
                        return true;
                }
            }
        }
        return false;
    }

    private boolean hasRemoveObserverMethod() {
        for (MethodNode m : classNode.methods) {
            String name = m.name.toLowerCase();
            if (name.contains("remove") || name.contains("unsubscribe")
                    || name.contains("detach") || name.contains("deregister")
                    || name.contains("unregister")) {
                return true;
            }
        }
        return false;
    }

    private boolean isObserverImplementation() {
        for (String iface : classNode.interfaces) {
            ClassNode ifaceNode = classMap.get(iface);
            if (ifaceNode == null)
                continue;
            for (MethodNode m : ifaceNode.methods) {
                if (isNotifyMethodName(m.name))
                    return true;
            }
        }
        return false;
    }

    private boolean isNotifyMethodName(String name) {
        String lower = name.toLowerCase();
        return lower.contains("update")
                || lower.contains("notify")
                || lower.contains("onchange")
                || lower.contains("onevent")
                || lower.contains("dispatch")
                || lower.contains("trigger")
                || lower.contains("publish")
                || lower.contains("broadcast")
                || lower.contains("fire");
    }
}
