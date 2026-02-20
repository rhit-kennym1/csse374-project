package testclasses.example;

/**
 * Negative test: should NOT trigger StrategyPatternLinter.
 *
 * No interface-typed strategy field that is injected and delegated through.
 */
public class TestNoStrategyPattern {

    static class Helper {
        int doThing(int x) {
            return x + 1;
        }
    }

    private final Helper helper = new Helper();

    public int compute(int x) {
        // Uses helper (created internally) and does not look like injected strategy delegation
        return helper.doThing(x);
    }

    public static void main(String[] args) {
        System.out.println(new TestNoStrategyPattern().compute(10));
    }
}
