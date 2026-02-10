package example;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class HollywoodPrincipleLinter implements PrincipleLinterInterface {

    private final ClassNode classNode;

    public HollywoodPrincipleLinter(ClassNode classNode) {
        this.classNode = classNode;
    }

	@Override
	public void lintClass() {
		// TODO Auto-generated method stub
		
	}
}
