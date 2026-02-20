package testclasses.example;

import java.util.Objects;

/**
 * Teammate-style test fixture for StrategyPatternLinter.
 *
 * SHOULD TRIGGER:
 *  - CheckoutContextCtor (strategy injected via constructor, delegated call via field)
 *  - CheckoutContextSetter (strategy injected via setter, delegated call via field)
 *
 * SHOULD NOT TRIGGER:
 *  - HasStrategyButNeverUsesIt (injected but never delegates)
 *  - CreatesStrategyInternally (delegates, but not injected from outside)
 *  - CallsParameterNotField (has field + injection, but delegates using parameter)
 */
public class TestStrategyPattern {

    // Strategy interface
    interface PaymentStrategy {
        int pay(int cents);
    }

    // Concrete strategies
    static class CashStrategy implements PaymentStrategy {
        @Override
        public int pay(int cents) {
            return cents; // no fee
        }
    }

    static class DiscountStrategy implements PaymentStrategy {
        private final int percentOff;

        DiscountStrategy(int percentOff) {
            this.percentOff = percentOff;
        }

        @Override
        public int pay(int cents) {
            return cents - (cents * percentOff / 100);
        }
    }

    // -------------------- SHOULD TRIGGER #1 --------------------
    static class CheckoutContextCtor {
        private final PaymentStrategy strategy;

        CheckoutContextCtor(PaymentStrategy strategy) {
            this.strategy = Objects.requireNonNull(strategy);
        }

        public int checkout(int cents) {
            // Delegation through FIELD (GETFIELD + ILOAD + INVOKEINTERFACE)
            return strategy.pay(cents);
        }
    }

    // -------------------- SHOULD TRIGGER #2 --------------------
    static class CheckoutContextSetter {
        private PaymentStrategy strategy;

        public void setStrategy(PaymentStrategy strategy) {
            this.strategy = strategy;
        }

        public int checkout(int cents) {
            return strategy.pay(cents);
        }
    }

    // -------------------- SHOULD NOT TRIGGER #1 --------------------
    // Injected, but never delegates through the field
    static class HasStrategyButNeverUsesIt {
        private final PaymentStrategy strategy;

        HasStrategyButNeverUsesIt(PaymentStrategy strategy) {
            this.strategy = strategy;
        }

        public int checkout(int cents) {
            // Uses local computation, not strategy
            return cents + 10;
        }
    }

    // -------------------- SHOULD NOT TRIGGER #2 --------------------
    // Delegates, but strategy is NOT injected from outside (constructed internally)
    static class CreatesStrategyInternally {
        private final PaymentStrategy strategy;

        CreatesStrategyInternally() {
            this.strategy = new CashStrategy();
        }

        public int checkout(int cents) {
            return strategy.pay(cents);
        }
    }

    // -------------------- SHOULD NOT TRIGGER #3 --------------------
    // Has field + injection, but delegates using PARAMETER, not FIELD
    static class CallsParameterNotField {
        private final PaymentStrategy strategy;

        CallsParameterNotField(PaymentStrategy strategy) {
            this.strategy = strategy;
        }

        public int checkoutWithOverride(PaymentStrategy override, int cents) {
            // Delegation uses PARAMETER receiver, not field receiver
            return override.pay(cents);
        }
    }

    // A tiny main so it runs normally too (not required for bytecode tests)
    public static void main(String[] args) {
        CheckoutContextCtor c1 = new CheckoutContextCtor(new DiscountStrategy(10));
        System.out.println(c1.checkout(100));

        CheckoutContextSetter c2 = new CheckoutContextSetter();
        c2.setStrategy(new CashStrategy());
        System.out.println(c2.checkout(100));

        HasStrategyButNeverUsesIt c3 = new HasStrategyButNeverUsesIt(new CashStrategy());
        System.out.println(c3.checkout(100));

        CreatesStrategyInternally c4 = new CreatesStrategyInternally();
        System.out.println(c4.checkout(100));

        CallsParameterNotField c5 = new CallsParameterNotField(new CashStrategy());
        System.out.println(c5.checkoutWithOverride(new DiscountStrategy(50), 100));
    }
}
