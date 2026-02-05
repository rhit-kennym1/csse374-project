package example;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class StrategyPatternLinter implements PatternLinterInterface {

    private final ClassNode classNode;

    public StrategyPatternLinter(ClassNode classNode) {
        this.classNode = classNode;
    }

	@Override
	public void lintClass() {
		// TODO Auto-generated method stub
		
	}
}
