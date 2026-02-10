package example;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.ClassReader;

public class DeadCodeTest {

    public static void main(String[] args) throws Exception {
        // Create a class with dead code using ASM
        byte[] bytecode = createClassWithDeadCode();
        
        // Load it into a ClassNode for the linter
        ClassReader reader = new ClassReader(bytecode);
        ClassNode classNode = new ClassNode();
        reader.accept(classNode, ClassReader.EXPAND_FRAMES);
        
        // Run the linter
        System.out.println("Running DeadCodeLinter on generated class...\n");
        DeadCodeLinter linter = new DeadCodeLinter(classNode);
        linter.lintClass();
    }

    private static byte[] createClassWithDeadCode() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        
        // Create class: public class DeadCodeExample
        cw.visit(
            Opcodes.V11,                    // Java 11
            Opcodes.ACC_PUBLIC,             // public
            "example/DeadCodeExample",      // class name
            null,                           // no generics
            "java/lang/Object",             // extends Object
            null                            // no interfaces
        );

        // Create method with dead code after RETURN
        createMethodWithDeadAfterReturn(cw);
        
        // Create method with dead code after THROW
        createMethodWithDeadAfterThrow(cw);
        
        // Create method with dead code after GOTO
        createMethodWithDeadAfterGoto(cw);
        
        // Create valid method (no dead code) for comparison
        createValidMethod(cw);

        cw.visitEnd();
        return cw.toByteArray();
    }

    private static void createMethodWithDeadAfterReturn(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(
            Opcodes.ACC_PUBLIC,
            "deadAfterReturn",
            "()I",          // returns int
            null,
            null
        );
        mv.visitCode();
        
        // Line 10: return 42;
        mv.visitLdcInsn(10);  // fake line number for testing
        mv.visitIntInsn(Opcodes.BIPUSH, 42);
        mv.visitInsn(Opcodes.IRETURN);
        
        // DEAD CODE: these instructions can never execute
        mv.visitIntInsn(Opcodes.BIPUSH, 99);   // push 99 (unreachable!)
        mv.visitVarInsn(Opcodes.ISTORE, 1);    // store to local (unreachable!)
        mv.visitInsn(Opcodes.IRETURN);          // return (unreachable!)
        
        mv.visitMaxs(1, 2);
        mv.visitEnd();
    }

    private static void createMethodWithDeadAfterThrow(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(
            Opcodes.ACC_PUBLIC,
            "deadAfterThrow",
            "()V",          // returns void
            null,
            null
        );
        mv.visitCode();
        
        // throw new RuntimeException("error");
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/RuntimeException");
        mv.visitInsn(Opcodes.DUP);
        mv.visitLdcInsn("error");
        mv.visitMethodInsn(
            Opcodes.INVOKESPECIAL,
            "java/lang/RuntimeException",
            "<init>",
            "(Ljava/lang/String;)V",
            false
        );
        mv.visitInsn(Opcodes.ATHROW);
        
        // DEAD CODE after throw
        mv.visitFieldInsn(
            Opcodes.GETSTATIC,
            "java/lang/System",
            "out",
            "Ljava/io/PrintStream;"
        );
        mv.visitLdcInsn("This never prints");
        mv.visitMethodInsn(
            Opcodes.INVOKEVIRTUAL,
            "java/io/PrintStream",
            "println",
            "(Ljava/lang/String;)V",
            false
        );
        mv.visitInsn(Opcodes.RETURN);
        
        mv.visitMaxs(3, 1);
        mv.visitEnd();
    }

    private static void createMethodWithDeadAfterGoto(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(
            Opcodes.ACC_PUBLIC,
            "deadAfterGoto",
            "()V",
            null,
            null
        );
        mv.visitCode();
        
        org.objectweb.asm.Label endLabel = new org.objectweb.asm.Label();
        
        // goto end (like an unconditional break)
        mv.visitJumpInsn(Opcodes.GOTO, endLabel);
        
        // DEAD CODE: between goto and its target
        mv.visitIntInsn(Opcodes.BIPUSH, 123);
        mv.visitVarInsn(Opcodes.ISTORE, 1);
        
        // end:
        mv.visitLabel(endLabel);
        mv.visitInsn(Opcodes.RETURN);
        
        mv.visitMaxs(1, 2);
        mv.visitEnd();
    }

    private static void createValidMethod(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(
            Opcodes.ACC_PUBLIC,
            "validMethod",
            "(Z)I",         // takes boolean, returns int
            null,
            null
        );
        mv.visitCode();
        
        org.objectweb.asm.Label elseLabel = new org.objectweb.asm.Label();
        org.objectweb.asm.Label endLabel = new org.objectweb.asm.Label();
        
        // if (flag)
        mv.visitVarInsn(Opcodes.ILOAD, 1);
        mv.visitJumpInsn(Opcodes.IFEQ, elseLabel);
        
        // return 1;
        mv.visitInsn(Opcodes.ICONST_1);
        mv.visitInsn(Opcodes.IRETURN);
        
        // else
        mv.visitLabel(elseLabel);
        
        // return 0;
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitInsn(Opcodes.IRETURN);
        
        mv.visitMaxs(1, 2);
        mv.visitEnd();
    }
}