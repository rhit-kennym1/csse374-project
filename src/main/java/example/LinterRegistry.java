package example;

import org.objectweb.asm.tree.ClassNode;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.BiFunction;

public class LinterRegistry {
    private static final Map<String, Function<ClassNode, Linter>> linters = new HashMap<>();
    private static final Map<String, BiFunction<ClassNode, Map<String, ClassNode>, Linter>> packageLinters = new HashMap<>();

    static {
        register("EqualsHashCode", EqualsHashCodeLinter::new);
        register("DeadCode", DeadCodeLinter::new);
        register("UnusedVariables", UnusedVariablesLinter::new);
        register("StrategyPattern", StrategyPatternLinter::new);
        register("HollywoodPrinciple", HollywoodPrincipleLinter::new);
        register("OpenClosedPrinciple", OpenClosedPrincipleLinter::new);
        register("DecoratorPattern", DecoratorPatternLinter::new);
        register("DemeterPrinciple", LawOfDemeterPrinciple::new);
        register("ObserverPattern", ObserverPatternLinter::new);
        register("FeatureEnvy", FeatureEnvyLinter::new);
        register("AdapterPattern", PatternAdapterLinter::new);
        register("TemporalCoupling", TemporalCouplingLinter::new);
        
        // Register package-wide linters only
        registerPackageLinter("CyclicDependency", CycleDependencyLinter::new);
    }

    public static void register(String name, Function<ClassNode, Linter> constructor) {
        linters.put(name, constructor);
    }

    public static void registerPackageLinter(String name, BiFunction<ClassNode, Map<String, ClassNode>, Linter> constructor) {
        packageLinters.put(name, constructor);
    }

    public static Linter create(String name, ClassNode classNode) {
        Function<ClassNode, Linter> constructor = linters.get(name);
        if (constructor == null) {
            throw new IllegalArgumentException("Unknown linter: " + name);
        }
        return constructor.apply(classNode);
    }

    public static Linter createPackageLinter(String name, ClassNode classNode, Map<String, ClassNode> allClasses) {
        BiFunction<ClassNode, Map<String, ClassNode>, Linter> constructor = packageLinters.get(name);
        if (constructor == null) {
            throw new IllegalArgumentException("Linter '" + name + "' does not support package-wide analysis");
        }
        return constructor.apply(classNode, allClasses);
    }
}