package example;

import org.objectweb.asm.tree.*;

import java.util.*;

/**
 * Detects cyclic dependencies between classes using Tarjan's Strongly Connected Components algorithm.
 * This linter analyzes method calls to find circular dependencies at the class level.
 */
public class CycleDependencyLinter implements Linter {
    private final ClassNode classNode;
    private final Map<String, ClassNode> allClasses;
    
    // For Tarjan's algorithm
    private int index = 0;
    private Stack<String> stack = new Stack<>();
    private Map<String, Integer> indices = new HashMap<>();
    private Map<String, Integer> lowLinks = new HashMap<>();
    private Map<String, Boolean> onStack = new HashMap<>();
    private List<List<String>> sccs = new ArrayList<>();

    public CycleDependencyLinter(ClassNode classNode) {
        this.classNode = classNode;
        this.allClasses = new HashMap<>();
        this.allClasses.put(classNode.name, classNode);
    }

    /**
     * Constructor that accepts multiple classes for package-wide analysis
     */
    public CycleDependencyLinter(ClassNode classNode, Map<String, ClassNode> allClasses) {
        this.classNode = classNode;
        this.allClasses = allClasses;
    }

    @Override
    public void lintClass() {
        // Build dependency graph
        Map<String, Set<String>> dependencyGraph = buildDependencyGraph();
        
        // Find SCCs using Tarjan's algorithm
        findStronglyConnectedComponents(dependencyGraph);
        
        // Report cycles
        reportCycles();
    }

    @Override
    public LinterType getType() {
        return LinterType.PRINCIPLE;
    }

    /**
     * Build a dependency graph where each class points to classes it depends on
     */
    private Map<String, Set<String>> buildDependencyGraph() {
        Map<String, Set<String>> graph = new HashMap<>();
        
        for (Map.Entry<String, ClassNode> entry : allClasses.entrySet()) {
            String className = entry.getKey();
            ClassNode node = entry.getValue();
            
            Set<String> dependencies = new HashSet<>();
            
            // Analyze all methods in the class
            if (node.methods != null) {
                for (MethodNode method : node.methods) {
                    // Skip constructors and static initializers
                    if (method.name.equals("<init>") || method.name.equals("<clinit>")) {
                        continue;
                    }
                    
                    // Find all method calls in this method
                    if (method.instructions != null) {
                        for (AbstractInsnNode insn : method.instructions) {
                            if (insn instanceof MethodInsnNode) {
                                MethodInsnNode methodCall = (MethodInsnNode) insn;
                                String targetClass = methodCall.owner;
                                
                                // Only track dependencies to classes we're analyzing
                                if (allClasses.containsKey(targetClass) && 
                                    !targetClass.equals(className)) {
                                    dependencies.add(targetClass);
                                }
                            }
                        }
                    }
                }
            }
            
            // Also check field types for dependencies
            if (node.fields != null) {
                for (FieldNode field : node.fields) {
                    String fieldType = extractClassFromDescriptor(field.desc);
                    if (fieldType != null && allClasses.containsKey(fieldType) && 
                        !fieldType.equals(className)) {
                        dependencies.add(fieldType);
                    }
                }
            }
            
            graph.put(className, dependencies);
        }
        
        return graph;
    }

    /**
     * Extract class name from a type descriptor
     */
    private String extractClassFromDescriptor(String descriptor) {
        if (descriptor.startsWith("L") && descriptor.endsWith(";")) {
            return descriptor.substring(1, descriptor.length() - 1);
        }
        return null;
    }

    /**
     * Find all strongly connected components using Tarjan's algorithm
     */
    private void findStronglyConnectedComponents(Map<String, Set<String>> graph) {
        for (String className : graph.keySet()) {
            if (!indices.containsKey(className)) {
                tarjanDFS(className, graph);
            }
        }
    }

    /**
     * Tarjan's DFS for finding SCCs
     */
    private void tarjanDFS(String className, Map<String, Set<String>> graph) {
        indices.put(className, index);
        lowLinks.put(className, index);
        index++;
        stack.push(className);
        onStack.put(className, true);

        // Visit all neighbors
        Set<String> neighbors = graph.get(className);
        if (neighbors != null) {
            for (String neighbor : neighbors) {
                if (!indices.containsKey(neighbor)) {
                    // Neighbor not yet visited
                    tarjanDFS(neighbor, graph);
                    lowLinks.put(className, Math.min(lowLinks.get(className), lowLinks.get(neighbor)));
                } else if (onStack.getOrDefault(neighbor, false)) {
                    // Neighbor is on stack, part of current SCC
                    lowLinks.put(className, Math.min(lowLinks.get(className), indices.get(neighbor)));
                }
            }
        }

        // If className is a root node, pop the stack to get SCC
        if (lowLinks.get(className).equals(indices.get(className))) {
            List<String> scc = new ArrayList<>();
            String node;
            do {
                node = stack.pop();
                onStack.put(node, false);
                scc.add(node);
            } while (!node.equals(className));
            
            // Only add SCCs with more than one class (cycles)
            if (scc.size() > 1) {
                sccs.add(scc);
            }
        }
    }

    /**
     * Report detected cycles
     */
    private void reportCycles() {
        if (sccs.isEmpty()) {
            System.out.println("No cyclic dependencies detected");
        } else {
            System.out.println("Cyclic dependencies detected:");
            for (int i = 0; i < sccs.size(); i++) {
                List<String> cycle = sccs.get(i);
                System.out.println("  Cycle " + (i + 1) + ": " + 
                    formatCycle(cycle));
            }
        }
    }

    /**
     * Format a cycle for display
     */
    private String formatCycle(List<String> cycle) {
        List<String> formatted = new ArrayList<>();
        for (String className : cycle) {
            formatted.add(simplifyClassName(className));
        }
        return String.join(" <-> ", formatted);
    }

    /**
     * Simplify class name for display
     */
    private String simplifyClassName(String className) {
        String simplified = className.replace('/', '.');
        // Remove package prefix if too long
        if (simplified.length() > 40) {
            int lastDot = simplified.lastIndexOf('.');
            if (lastDot > 0) {
                simplified = simplified.substring(lastDot + 1);
            }
        }
        return simplified;
    }
}