package example;

import org.objectweb.asm.tree.ClassNode;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class LinterRegistry {
    private static final Map<String, Function<ClassNode, Linter>> linters = new HashMap<>();

    static {
        register("EqualsHashCode", EqualsHashCodeLinter::new);
        register("DeadCode", DeadCodeLinter::new);
        register("UnusedVariables", UnusedVariablesLinter::new);
        register("OpenClosedPrinciple", OpenClosedPrincipleLinter::new);
        register("DecoratorPattern", DecoratorPatternLinter::new);
        register("DemeterPrinciple", LawOfDemeterPrinciple::new);
        register("ObserverPattern", ObserverPatternLinter::new);
        register("FeatureEnvy", FeatureEnvyLinter::new);
        register("AdapterPattern", PatternAdapterLinter::new);
        
    }

    public static void register(String name, Function<ClassNode, Linter> constructor) {
        linters.put(name, constructor);
    }

    public static Linter create(String name, ClassNode classNode) {
        Function<ClassNode, Linter> constructor = linters.get(name);
        if (constructor == null) {
            throw new IllegalArgumentException("Unknown linter: " + name);
        }
        return constructor.apply(classNode);
    }
}