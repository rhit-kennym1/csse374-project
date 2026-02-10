package example;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

public class DeadCodeLinter implements CheckstyleLinterInterface {
    private final ClassNode classNode;

    public DeadCodeLinter(ClassNode classNode) {
        this.classNode = classNode;
    }

    @Override
    public void lintClass() {
        for (MethodNode method : classNode.methods) {
            checkMethodForDeadCode(method);
        }
    }

    private void checkMethodForDeadCode(MethodNode method) {
        InsnList instructions = method.instructions;
        
        for (int i = 0; i < instructions.size() - 1; i++) {
            AbstractInsnNode current = instructions.get(i);
            AbstractInsnNode next = instructions.get(i + 1);
            
            if (isTerminalInstruction(current) && !isValidAfterTerminal(next)) {
                int line = findLineNumber(method, current);
                String lineInfo = line > 0 ? " (line " + line + ")" : "";
                System.err.println(classNode.name + "." + method.name + method.desc 
                    + lineInfo + ": unreachable code after " + getOpcodeName(current.getOpcode()));
            }
        }
    }

    private boolean isTerminalInstruction(AbstractInsnNode insn) {
        int opcode = insn.getOpcode();
        //Check for all cases of return, throw, and break/continue
        return opcode == Opcodes.RETURN
            || opcode == Opcodes.IRETURN
            || opcode == Opcodes.LRETURN
            || opcode == Opcodes.FRETURN
            || opcode == Opcodes.DRETURN
            || opcode == Opcodes.ARETURN
            || opcode == Opcodes.ATHROW
            || opcode == Opcodes.GOTO;
    }

    private boolean isValidAfterTerminal(AbstractInsnNode insn) {
        //There are certain things that appear in bytecode after a return that are valid - Labels (jump targets when return is in a conditional), frames (metadata inserted by compiler), Line numbers (debug info)
        return insn instanceof LabelNode || insn instanceof FrameNode || insn instanceof LineNumberNode;
    }

    private int findLineNumber(MethodNode method, AbstractInsnNode target) {
        // Walk backwards to find the most recent line number
        AbstractInsnNode current = target;
        while (current != null) {
            if (current instanceof LineNumberNode) {
                return ((LineNumberNode) current).line;
            }
            current = current.getPrevious();
        }
        return -1;
    }

    private String getOpcodeName(int opcode) {
        switch (opcode) {
            case Opcodes.RETURN:
            case Opcodes.IRETURN:
            case Opcodes.LRETURN:
            case Opcodes.FRETURN:
            case Opcodes.DRETURN:
            case Opcodes.ARETURN:
                return "return";
            case Opcodes.ATHROW:
                return "throw";
            case Opcodes.GOTO:
                return "goto";
            default:
                return "terminal instruction";
        }
    }
}