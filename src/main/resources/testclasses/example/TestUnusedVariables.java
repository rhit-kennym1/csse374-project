package example;

import java.util.ArrayList;
import java.util.List;

/**
 * Test fixture for UnusedVariablesLinter.
 *
 * Expected flags (assuming your linter checks):
 *  - unused private field: unusedPrivateField
 *  - unused private method: unusedPrivateMethod
 *  - unused locals: unusedLocalInCtor, unusedLocalInMethod, unusedLocalShadow
 *
 * Expected NOT flagged:
 *  - usedPrivateField, names, calledPrivateMethod, used locals
 */
public class TestUnusedVariables {

    // Should be flagged: private and never referenced anywhere.
    private int unusedPrivateField = 123;

    // Should NOT be flagged: read in validMethod().
    private int usedPrivateField = 42;

    // Should NOT be flagged: used in validMethod().
    private final List<String> names;

    public TestUnusedVariables() {
        names = new ArrayList<>();
        names.add("Alice");

        // Should be flagged: stored but never loaded.
        int unusedLocalInCtor = 999;

        // Should NOT be flagged: used.
        int usedLocalInCtor = 10;
        System.out.println("usedLocalInCtor=" + usedLocalInCtor);
    }

    public void validMethod() {
        // Keep usedPrivateField and names "used"
        System.out.println("usedPrivateField=" + usedPrivateField);
        System.out.println("names size=" + names.size());

        // Used local -> should NOT be flagged
        int usedLocal = 5;
        System.out.println(usedLocal);

        // Unused locals -> should be flagged
        String unusedLocalInMethod = "hello";

        // Another unused local (shadow-like case)
        int unusedLocalShadow = 7;
        if (usedLocal > 0) {
            // a used temp inside a branch
            int branchUsed = usedLocal + 1;
            System.out.println(branchUsed);
        }
    }

    public void callsPrivateMethod() {
        // Ensures this private method is considered "used"
        calledPrivateMethod();
    }

    // Should NOT be flagged: called by callsPrivateMethod().
    private void calledPrivateMethod() {
        System.out.println("I am called");
    }

    // Should be flagged: private and never called.
    private void unusedPrivateMethod() {
        System.out.println("I am never called");
    }

    public static void main(String[] args) {
        TestUnusedVariables t = new TestUnusedVariables();
        t.validMethod();
        t.callsPrivateMethod();
    }
}
