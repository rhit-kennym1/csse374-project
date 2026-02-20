package testclasses.example;

/**
 * UnusedVariablesLinter test: UNUSED LOCAL VARIABLES.
 *
 * Expected: linter should report:
 *  - unusedLocalInCtor
 *  - unusedLocalInMethod
 */
public class TestUnusedLocalVariables {

    public TestUnusedLocalVariables() {
        // SHOULD TRIGGER: stored but never loaded
        int unusedLocalInCtor = 42;

        // SHOULD NOT TRIGGER: used
        int usedLocalInCtor = 10;
        System.out.println(usedLocalInCtor);
    }

    public void doWork() {
        // SHOULD TRIGGER: stored but never loaded
        String unusedLocalInMethod = "hello";

        // SHOULD NOT TRIGGER: used
        String used = "world";
        System.out.println(used.length());
    }
}
