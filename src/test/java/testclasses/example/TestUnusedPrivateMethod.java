package testclasses.example;

/**
 * UnusedVariablesLinter test: UNUSED PRIVATE METHOD only.
 *
 * Expected: linter should report the private method 'unusedHelper'.
 *
 * Notes:
 * - Ensure locals are used.
 * - Ensure fields (if any) are used.
 */
public class TestUnusedPrivateMethod {

    public void run() {
        // Use local so it isn't reported
        int usedLocal = 1;
        System.out.println(usedLocal);

        // Call used helper so it isn't reported
        usedHelper();
    }

    // SHOULD NOT TRIGGER: called
    private void usedHelper() {
        System.out.println("used");
    }

    // TRIGGER: never called
    private void unusedHelper() {
        System.out.println("unused");
    }
}
