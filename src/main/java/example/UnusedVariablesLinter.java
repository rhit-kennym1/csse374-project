package example;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class UnusedVariablesLinter implements  Linter {

    private final ClassNode classNode;

    public UnusedVariablesLinter(ClassNode classNode) {
        this.classNode = classNode;
    }
    
    @Override
    public LinterType getType() {
        return LinterType.CHECKSTYLE;
    }
    
	@Override
	public void lintClass() {
		// TODO Auto-generated method stu
		
	}
}
