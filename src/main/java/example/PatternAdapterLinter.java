package example;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.*;

public class PatternAdapterLinter implements Linter {
    private final ClassNode classNode;

    public PatternAdapterLinter(ClassNode classNode) {
        this.classNode = classNode;
    }

    @Override
    public void lintClass() {
        AdapterInfo adapterInfo = detectAdapterPattern();
        
        if (adapterInfo != null) {
            System.out.println("Adapter pattern detected: " + adapterInfo.adapterClass + 
                             " adapts " + adapterInfo.adapteeType + 
                             " to " + adapterInfo.targetInterface);
        } else {
            System.out.println("No Adapter pattern detected");
        }
    }

    @Override
    public LinterType getType() {
        return LinterType.PATTERN;
    }

    private AdapterInfo detectAdapterPattern() {
        // Step 1: Check if class implements an interface or extends an abstract class
        List<String> targetInterfaces = getTargetInterfaces();
        if (targetInterfaces.isEmpty()) {
            return null;
        }

        // Step 2: Find adaptee field (a field whose type is different from the class hierarchy)
        FieldNode adapteeField = findAdapteeField(targetInterfaces);
        if (adapteeField == null) {
            return null;
        }

        String adapteeType = getTypeFromDescriptor(adapteeField.desc);

        // Step 3: Verify adaptee is not in the same inheritance hierarchy
        if (isInSameHierarchy(adapteeType, targetInterfaces)) {
            return null;
        }

        // Step 4: Check if constructor takes adaptee as parameter and assigns it
        if (!constructorInitializesAdaptee(adapteeField, adapteeType)) {
            return null;
        }

        // Step 5: Check if methods delegate to the adaptee
        List<String> delegatingMethods = findDelegatingMethods(adapteeField.name, adapteeType);
        if (delegatingMethods.isEmpty()) {
            return null;
        }

        // All conditions met - return adapter info
        return new AdapterInfo(
            targetInterfaces.get(0),
            classNode.name.replace('/', '.'),
            adapteeType,
            delegatingMethods
        );
    }

    /**
     * Get list of interfaces or abstract parent classes that this class implements/extends
     */
    private List<String> getTargetInterfaces() {
        List<String> targets = new ArrayList<>();

        // Add all interfaces
        if (classNode.interfaces != null) {
            for (String interfaceName : classNode.interfaces) {
                targets.add(interfaceName.replace('/', '.'));
            }
        }

        // Check if extends an abstract class (simplified check)
        if (classNode.superName != null && !classNode.superName.equals("java/lang/Object")) {
            targets.add(classNode.superName.replace('/', '.'));
        }

        return targets;
    }

    /**
     * Find a field that could be the adaptee
     * The adaptee should be a non-primitive object type
     */
    private FieldNode findAdapteeField(List<String> targetInterfaces) {
        if (classNode.fields == null) {
            return null;
        }

        for (FieldNode field : classNode.fields) {
            // Skip static fields
            if ((field.access & Opcodes.ACC_STATIC) != 0) {
                continue;
            }

            // Check if it's an object type (starts with 'L')
            if (field.desc.startsWith("L")) {
                String fieldType = getTypeFromDescriptor(field.desc);
                
                // Should not be the same as any target interface
                if (!targetInterfaces.contains(fieldType)) {
                    return field;
                }
            }
        }

        return null;
    }

    /**
     * Extract class name from type descriptor
     * Example: "Ljava/lang/String;" -> "java.lang.String"
     */
    private String getTypeFromDescriptor(String descriptor) {
        if (descriptor.startsWith("L") && descriptor.endsWith(";")) {
            return descriptor.substring(1, descriptor.length() - 1).replace('/', '.');
        }
        return descriptor;
    }

    /**
     * Check if adaptee is in the same inheritance hierarchy as target interfaces
     * Simplified version - just checks for exact match
     */
    private boolean isInSameHierarchy(String adapteeType, List<String> targetInterfaces) {
        return targetInterfaces.contains(adapteeType);
    }

    /**
     * Check if any constructor takes the adaptee type as parameter and assigns it to the field
     */
    private boolean constructorInitializesAdaptee(FieldNode adapteeField, String adapteeType) {
        if (classNode.methods == null) {
            return false;
        }

        for (MethodNode method : classNode.methods) {
            // Only check constructors
            if (!method.name.equals("<init>")) {
                continue;
            }

            // Check if constructor has parameter matching adaptee type
            if (!hasParameterOfType(method, adapteeType)) {
                continue;
            }

            // Check if constructor assigns to the adaptee field
            if (assignsToField(method, adapteeField.name)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if method has a parameter of the specified type
     */
    private boolean hasParameterOfType(MethodNode method, String typeName) {
        String typeDescriptor = "L" + typeName.replace('.', '/') + ";";
        return method.desc.contains(typeDescriptor);
    }

    /**
     * Check if method assigns to a specific field
     */
    private boolean assignsToField(MethodNode method, String fieldName) {
        if (method.instructions == null) {
            return false;
        }

        for (AbstractInsnNode insn : method.instructions) {
            if (insn instanceof FieldInsnNode) {
                FieldInsnNode fieldInsn = (FieldInsnNode) insn;
                if (fieldInsn.getOpcode() == Opcodes.PUTFIELD && 
                    fieldInsn.name.equals(fieldName)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Find methods that delegate to the adaptee field
     */
    private List<String> findDelegatingMethods(String adapteeFieldName, String adapteeType) {
        List<String> delegating = new ArrayList<>();

        if (classNode.methods == null) {
            return delegating;
        }

        for (MethodNode method : classNode.methods) {
            // Skip constructors and static methods
            if (method.name.equals("<init>") || method.name.equals("<clinit>") ||
                (method.access & Opcodes.ACC_STATIC) != 0) {
                continue;
            }

            // Check if method accesses the adaptee field and invokes methods on it
            if (delegatesToField(method, adapteeFieldName)) {
                delegating.add(method.name);
            }
        }

        return delegating;
    }

    /**
     * Check if a method delegates to a specific field by:
     * 1. Getting the field (GETFIELD)
     * 2. Invoking a method on it (INVOKEVIRTUAL, INVOKEINTERFACE, etc.)
     */
    private boolean delegatesToField(MethodNode method, String fieldName) {
        if (method.instructions == null) {
            return false;
        }

        boolean foundFieldAccess = false;
        
        for (AbstractInsnNode insn : method.instructions) {
            // Check for field access
            if (insn instanceof FieldInsnNode) {
                FieldInsnNode fieldInsn = (FieldInsnNode) insn;
                if (fieldInsn.getOpcode() == Opcodes.GETFIELD && 
                    fieldInsn.name.equals(fieldName)) {
                    foundFieldAccess = true;
                }
            }
            
            // Check for method invocation after field access
            if (foundFieldAccess && insn instanceof MethodInsnNode) {
                MethodInsnNode methodInsn = (MethodInsnNode) insn;
                // Any method call after getting the field indicates delegation
                if (methodInsn.getOpcode() == Opcodes.INVOKEVIRTUAL ||
                    methodInsn.getOpcode() == Opcodes.INVOKEINTERFACE ||
                    methodInsn.getOpcode() == Opcodes.INVOKESPECIAL) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Data class to hold adapter pattern information
     */
    private static class AdapterInfo {
        final String targetInterface;
        final String adapterClass;
        final String adapteeType;
        final List<String> delegatingMethods;

        AdapterInfo(String targetInterface, String adapterClass, 
                   String adapteeType, List<String> delegatingMethods) {
            this.targetInterface = targetInterface;
            this.adapterClass = adapterClass;
            this.adapteeType = adapteeType;
            this.delegatingMethods = delegatingMethods;
        }
    }
}