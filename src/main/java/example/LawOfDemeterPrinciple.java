package example;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.HashSet;
import java.util.Set;

public class LawOfDemeterPrinciple implements Linter {
    private final ClassNode classNode;
    
    // Common exceptions - builders, streams, strings
    private static final Set<String> EXCLUDED_PATTERNS = new HashSet<>();
    
    static {
        
        EXCLUDED_PATTERNS.add("Builder");
        EXCLUDED_PATTERNS.add("builder");
        
        
        EXCLUDED_PATTERNS.add("java/util/stream/");
        EXCLUDED_PATTERNS.add("stream");
        EXCLUDED_PATTERNS.add("filter");
        EXCLUDED_PATTERNS.add("map");
        EXCLUDED_PATTERNS.add("collect");
        EXCLUDED_PATTERNS.add("forEach");
        
        
        EXCLUDED_PATTERNS.add("java/lang/String");
        EXCLUDED_PATTERNS.add("java/lang/StringBuilder");
        EXCLUDED_PATTERNS.add("java/lang/StringBuffer");
        
       
    }

    public LawOfDemeterPrinciple(ClassNode classNode) {
        this.classNode = classNode;
    }

    @Override
    public LinterType getType() {
        return LinterType.PRINCIPLE;
    }

    @Override
    public void lintClass() {
        for (MethodNode method : classNode.methods) {
            checkMethodForViolations(method);
        }
    }

    private void checkMethodForViolations(MethodNode method) {
        if (method.name.equals("<init>") || method.name.equals("<clinit>")) {
            return;
        }

        Set<Integer> allowedLocalVars = new HashSet<>();
        
        if ((method.access & Opcodes.ACC_STATIC) == 0) {
            allowedLocalVars.add(0);
        }
        
        int paramCount = getParameterCount(method.desc, (method.access & Opcodes.ACC_STATIC) != 0);
        for (int i = 0; i <= paramCount; i++) {
            allowedLocalVars.add(i);
        }

        int chainDepth = 0;
        AbstractInsnNode previousInsn = null;

        InsnList instructions = method.instructions;
        for (int i = 0; i < instructions.size(); i++) {
            AbstractInsnNode insn = instructions.get(i);

            
            if (insn instanceof VarInsnNode) {
                VarInsnNode varInsn = (VarInsnNode) insn;
                if (isStoreInstruction(varInsn.getOpcode())) {
                    allowedLocalVars.add(varInsn.var);
                    chainDepth = 0;
                }
            }

           
            if (insn instanceof MethodInsnNode) {
                MethodInsnNode methodInsn = (MethodInsnNode) insn;
                
               
                if (isExcluded(methodInsn)) {
                    continue;
                }

                
                if (previousInsn instanceof MethodInsnNode) {
                    chainDepth++;
                    
                    if (chainDepth > 1) {
                        int line = findLineNumber(method, insn);
                        String lineInfo = line > 0 ? " (line " + line + ")" : "";
                        System.err.println(classNode.name + "." + method.name + method.desc 
                                + lineInfo + ": Law of Demeter violation - chain depth " 
                                + chainDepth + " calling " + methodInsn.owner + "." + methodInsn.name);
                    }
                } else {
                    // New chain starting
                    chainDepth = 1;
                }
                
               
                if (previousInsn instanceof FieldInsnNode) {
                    FieldInsnNode fieldInsn = (FieldInsnNode) previousInsn;
                    if (fieldInsn.getOpcode() == Opcodes.GETFIELD) {
                       
                    }
                }

                previousInsn = insn;
            } else if (insn.getOpcode() >= 0) {
              
                if (!isStackManipulation(insn.getOpcode())) {
                    previousInsn = insn;
                    if (isValueProducer(insn)) {
                        chainDepth = 0;
                    }
                }
            }
        }
    }

    private boolean isExcluded(MethodInsnNode methodInsn) {
        // Check owner class
        for (String pattern : EXCLUDED_PATTERNS) {
            if (methodInsn.owner.contains(pattern)) {
                return true;
            }
            if (methodInsn.name.contains(pattern)) {
                return true;
            }
        }
        
        if (methodInsn.name.startsWith("set") && 
            methodInsn.desc.endsWith(")L" + methodInsn.owner + ";")) {
            return true;
        }
        
        return false;
    }

    private boolean isStoreInstruction(int opcode) {
        return opcode == Opcodes.ASTORE
                || opcode == Opcodes.ISTORE
                || opcode == Opcodes.LSTORE
                || opcode == Opcodes.FSTORE
                || opcode == Opcodes.DSTORE;
    }

    private boolean isStackManipulation(int opcode) {
        return opcode == Opcodes.DUP
                || opcode == Opcodes.DUP2
                || opcode == Opcodes.DUP_X1
                || opcode == Opcodes.DUP_X2
                || opcode == Opcodes.DUP2_X1
                || opcode == Opcodes.DUP2_X2
                || opcode == Opcodes.SWAP
                || opcode == Opcodes.POP
                || opcode == Opcodes.POP2;
    }

    private boolean isValueProducer(AbstractInsnNode insn) {
        int opcode = insn.getOpcode();
       
        return opcode == Opcodes.ALOAD
                || opcode == Opcodes.ILOAD
                || opcode == Opcodes.LLOAD
                || opcode == Opcodes.FLOAD
                || opcode == Opcodes.DLOAD
                || opcode == Opcodes.GETSTATIC
                || opcode == Opcodes.GETFIELD
                || opcode == Opcodes.NEW
                || opcode == Opcodes.LDC
                || (opcode >= Opcodes.ICONST_M1 && opcode <= Opcodes.DCONST_1);
    }

    private int getParameterCount(String desc, boolean isStatic) {
        int count = isStatic ? 0 : 1;
        int i = 1;
        while (desc.charAt(i) != ')') {
            char c = desc.charAt(i);
            if (c == 'L') {
                
                while (desc.charAt(i) != ';') i++;
                count++;
            } else if (c == '[') {
               
            } else if (c == 'D' || c == 'J') {
                
                count += 2;
            } else {
                
                count++;
            }
            i++;
        }
        return count;
    }

    private int findLineNumber(MethodNode method, AbstractInsnNode target) {
        AbstractInsnNode current = target;
        while (current != null) {
            if (current instanceof LineNumberNode) {
                return ((LineNumberNode) current).line;
            }
            current = current.getPrevious();
        }
        return -1;
    }
}