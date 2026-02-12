package example;

import java.util.*;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class SingleResponsibilityPrincipleLinter
        implements Linter {

    private ClassNode classNode;
    private List<String> warnings = new ArrayList<>();

    private static final int METHOD_THRESHOLD = 20;
    private static final int FIELD_THRESHOLD = 15;
    private static final int DEP_THRESHOLD = 10;

    public SingleResponsibilityPrincipleLinter(ClassNode classNode) {
        this.classNode = classNode;
    }
    
    @Override
    public LinterType getType() {
        return LinterType.PRINCIPLE;
    }
    
    @Override
    public void lintClass() {

        checkMethodCount();
        checkFieldCount();
        checkDependencies();
        checkCohesion();

        warnings.forEach(System.out::println);
    }

    private void checkMethodCount() {
        if (classNode.methods.size() > METHOD_THRESHOLD) {
            warnings.add("[SRP] Too many methods in " + classNode.name);
        }
    }

    private void checkFieldCount() {
        if (classNode.fields.size() > FIELD_THRESHOLD) {
            warnings.add("[SRP] Too many fields in " + classNode.name);
        }
    }

    private void checkDependencies() {
        Set<String> deps = new HashSet<>();

        for (MethodNode m : classNode.methods) {
            for (AbstractInsnNode insn : m.instructions) {
                if (insn instanceof MethodInsnNode) {
                    MethodInsnNode methodInsn = (MethodInsnNode) insn;
                    deps.add(methodInsn.owner);
                }
            }
        }

        if (deps.size() > DEP_THRESHOLD) {
            warnings.add("[SRP] Too many dependencies in " + classNode.name);
        }
    }

    private void checkCohesion() {
        Map<String, Set<String>> methodFieldUsage = new HashMap<>();

        for (MethodNode m : classNode.methods) {
            Set<String> used = new HashSet<>();

            for (AbstractInsnNode insn : m.instructions) {
                if (insn instanceof FieldInsnNode) {
                    FieldInsnNode fieldInsn = (FieldInsnNode) insn;
                    used.add(fieldInsn.name);
                }
            }

            methodFieldUsage.put(m.name, used);
        }

        int unrelatedPairs = 0;
        List<Set<String>> usages = new ArrayList<>(methodFieldUsage.values());

        for (int i = 0; i < usages.size(); i++) {
            for (int j = i + 1; j < usages.size(); j++) {
                Set<String> a = usages.get(i);
                Set<String> b = usages.get(j);

                Set<String> intersect = new HashSet<>(a);
                intersect.retainAll(b);

                if (intersect.isEmpty())
                    unrelatedPairs++;
            }
        }

        if (unrelatedPairs > usages.size()) {
            warnings.add("[SRP] Low cohesion in " + classNode.name);
        }
    }
}