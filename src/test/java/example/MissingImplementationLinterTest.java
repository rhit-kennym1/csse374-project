package example;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.ClassNode;

import java.io.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.objectweb.asm.Opcodes.*;

class MissingImplementationLinterTest {

    private ClassNode makeInterface(String name, String... methodNames) {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V11, ACC_PUBLIC | ACC_ABSTRACT | ACC_INTERFACE, name, null, "java/lang/Object", null);
        for (String m : methodNames) {
            cw.visitMethod(ACC_PUBLIC | ACC_ABSTRACT, m, "()V", null, null).visitEnd();
        }
        cw.visitEnd();
        ClassNode cn = new ClassNode();
        new ClassReader(cw.toByteArray()).accept(cn, 0);
        return cn;
    }

    private ClassNode makeClass(String name, String iface, String... implemented) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(V11, ACC_PUBLIC, name, null, "java/lang/Object", new String[] { iface });
        MethodVisitor init = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(ALOAD, 0);
        init.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        init.visitInsn(RETURN);
        init.visitMaxs(1, 1);
        init.visitEnd();
        for (String m : implemented) {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, m, "()V", null, null);
            mv.visitCode();
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 1);
            mv.visitEnd();
        }
        cw.visitEnd();
        ClassNode cn = new ClassNode();
        new ClassReader(cw.toByteArray()).accept(cn, 0);
        return cn;
    }

    private String capture(Runnable r) {
        PrintStream original = System.out;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buf));
        try {
            r.run();
        } finally {
            System.setOut(original);
        }
        return buf.toString();
    }

    @Test
    void detectsTwoMissingMethods() {
        ClassNode iface = makeInterface("test/Auditable", "create", "update", "delete");
        ClassNode partial = makeClass("test/PartialAuditable", "test/Auditable", "create");

        Map<String, ClassNode> classMap = new HashMap<>();
        classMap.put(iface.name, iface);
        classMap.put(partial.name, partial);

        String out = capture(() -> new MissingImplementationLinter(partial, classMap).lintClass());

        assertTrue(out.contains("update"), "Should flag missing update()");
        assertTrue(out.contains("delete"), "Should flag missing delete()");
        assertFalse(out.contains("create"), "Should not flag create() which is implemented");
    }

    @Test
    void noViolationsWhenAllMethodsImplemented() {
        ClassNode iface = makeInterface("test/Workable", "doWork", "cleanup");
        ClassNode full = makeClass("test/FullWorker", "test/Workable", "doWork", "cleanup");

        Map<String, ClassNode> classMap = new HashMap<>();
        classMap.put(iface.name, iface);
        classMap.put(full.name, full);

        String out = capture(() -> new MissingImplementationLinter(full, classMap).lintClass());

        assertTrue(out.isBlank(), "Expected no violations for complete implementation, got: " + out);
    }
}
