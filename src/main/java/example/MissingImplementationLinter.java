package example;

import java.util.*;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class MissingImplementationLinter implements CheckstyleLinterInterface {

    private ClassNode classNode;
    private Map<String, ClassNode> classMap;
    private List<String> warnings = new ArrayList<>();

    public MissingImplementationLinter(ClassNode classNode,
            Map<String, ClassNode> classMap) {
        this.classNode = classNode;
        this.classMap = classMap;
    }

    @Override
    public void lintClass() {
        if (isAbstract(classNode))
            return;

        Set<String> required = new HashSet<>();
        collectAbstractMethods(classNode, required);

        Set<String> implemented = getImplementedMethods(classNode);

        for (String sig : required) {
            if (!implemented.contains(sig)) {
                warnings.add("Missing implementation: " + sig
                        + " in " + classNode.name);
            }
        }

        warnings.forEach(System.out::println);
    }

    private void collectAbstractMethods(ClassNode node, Set<String> required) {
        // interfaces
        for (String iface : node.interfaces) {
            ClassNode ifaceNode = classMap.get(iface);
            if (ifaceNode != null) {
                for (MethodNode m : ifaceNode.methods) {
                    required.add(sig(m));
                }
            }
        }

        // parent abstract class
        if (node.superName != null) {
            ClassNode parent = classMap.get(node.superName);
            if (parent != null) {
                for (MethodNode m : parent.methods) {
                    if ((m.access & Opcodes.ACC_ABSTRACT) != 0) {
                        required.add(sig(m));
                    }
                }
            }
        }
    }

    private Set<String> getImplementedMethods(ClassNode node) {
        Set<String> set = new HashSet<>();
        for (MethodNode m : node.methods) {
            if ((m.access & Opcodes.ACC_ABSTRACT) == 0) {
                set.add(sig(m));
            }
        }
        return set;
    }

    private boolean isAbstract(ClassNode node) {
        return (node.access & Opcodes.ACC_ABSTRACT) != 0
                || (node.access & Opcodes.ACC_INTERFACE) != 0;
    }

    private String sig(MethodNode m) {
        return m.name + m.desc;
    }
}
