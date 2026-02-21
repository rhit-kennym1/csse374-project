package example;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import java.util.HashMap;
import java.util.Map;

public class MissingImplementationTest {

    public static void main(String[] args) throws Exception {
        Map<String, ClassNode> classMap = new HashMap<>();

        ClassNode iVehicle = buildInterface("test/IVehicle", "drive", "refuel", "park");
        classMap.put(iVehicle.name, iVehicle);

        ClassNode incomplete = buildConcreteClass(
                "test/IncompleteVehicle",
                "java/lang/Object",
                new String[] { "test/IVehicle" },
                "drive");
        classMap.put(incomplete.name, incomplete);
        new MissingImplementationLinter(incomplete, classMap).lintClass();

        ClassNode complete = buildConcreteClass(
                "test/CompleteVehicle",
                "java/lang/Object",
                new String[] { "test/IVehicle" },
                "drive", "refuel", "park");
        classMap.put(complete.name, complete);
        new MissingImplementationLinter(complete, classMap).lintClass();
    }

    private static ClassNode buildInterface(String internalName, String... methodNames) {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(
                Opcodes.V11,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE,
                internalName,
                null,
                "java/lang/Object",
                null);
        for (String method : methodNames) {
            cw.visitMethod(
                    Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT,
                    method,
                    "()V",
                    null,
                    null).visitEnd();
        }
        cw.visitEnd();
        return toClassNode(cw.toByteArray());
    }

    private static ClassNode buildConcreteClass(String internalName, String superName,
            String[] interfaces, String... implementedMethods) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V11, Opcodes.ACC_PUBLIC, internalName, null, superName, interfaces);

        for (String method : implementedMethods) {
            MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, method, "()V", null, null);
            mv.visitCode();
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(0, 1);
            mv.visitEnd();
        }

        cw.visitEnd();
        return toClassNode(cw.toByteArray());
    }

    private static ClassNode toClassNode(byte[] bytes) {
        ClassReader cr = new ClassReader(bytes);
        ClassNode cn = new ClassNode();
        cr.accept(cn, ClassReader.EXPAND_FRAMES);
        return cn;
    }
}
