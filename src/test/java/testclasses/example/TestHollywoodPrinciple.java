package testclasses.example;

import java.util.ArrayList;
import java.util.List;

/**
 * Test class for HollywoodPrincipleLinter.
 *
 * Your HollywoodPrincipleLinter flags a METHOD when at least 2 of these are high:
 *  - NEW count (>= 5) for non-java classes
 *  - getter-like calls (>= 8): getX/isX/hasX
 *  - condition count (>= 6): if/switch
 *
 * TRIGGER:
 *  - Orchestrator.orchestrate(): high NEW + high getters + high conditions
 *
 * SHOULD NOT TRIGGER:
 *  - LowComplexity.doWork(): low counts overall
 *  - GetterHeavyOnly.getterHeavy(): high getters but low NEW/conditions
 */
public class TestHollywoodPrinciple {

    // ----------------------------
    // TRIGGER
    // ----------------------------
    class Orchestrator {
        private final Model model = new Model();

        public void orchestrate() {
            // NEW count: create 6 project-class objects
            WorkerA a = new WorkerA();
            WorkerB b = new WorkerB();
            WorkerC c = new WorkerC();
            WorkerD d = new WorkerD();
            WorkerE e = new WorkerE();
            WorkerF f = new WorkerF();

            // Data pull count: 9 getter-like calls
            int x1 = model.getX1();
            int x2 = model.getX2();
            int x3 = model.getX3();
            int x4 = model.getX4();
            int x5 = model.getX5();
            int x6 = model.getX6();
            boolean ok = model.isOk();
            boolean ready = model.hasReadyFlag();
            String name = model.getName();

            // Condition count: >= 6 if statements (each compiles to at least one IF*)
            if (x1 > 0) a.run();
            if (x2 > 0) b.run();
            if (x3 > 0) c.run();
            if (x4 > 0) d.run();
            if (x5 > 0) e.run();
            if (x6 > 0) f.run();

            // a couple extra conditions
            if (ok && ready) {
                // local decision logic
                a.run();
            } else {
                b.run();
            }

            // use name so compiler doesn't optimize away
            if (name.length() == 0) {
                c.run();
            }
        }
    }

    // ----------------------------
    // SHOULD NOT TRIGGER (low complexity)
    // ----------------------------
    class LowComplexity {
        public void doWork() {
            Model m = new Model(); // 1 NEW
            if (m.getX1() > 0) {   // 1 getter + 1 condition
                System.out.println("ok");
            }
        }
    }

    // ----------------------------
    // SHOULD NOT TRIGGER (getter-heavy but low new/conditions)
    // ----------------------------
    class GetterHeavyOnly {
        private final Model model = new Model();

        public int getterHeavy() {
            // Many getters, but no NEWs in this method and minimal conditions
            int sum = 0;
            sum += model.getX1();
            sum += model.getX2();
            sum += model.getX3();
            sum += model.getX4();
            sum += model.getX5();
            sum += model.getX6();
            sum += model.getX7();
            sum += model.getX8();
            sum += model.getX9();
            return sum;
        }
    }

    // ----------------------------
    // Helper classes (project classes, count towards NEW)
    // ----------------------------
    class WorkerA { void run() {} }
    class WorkerB { void run() {} }
    class WorkerC { void run() {} }
    class WorkerD { void run() {} }
    class WorkerE { void run() {} }
    class WorkerF { void run() {} }

    // Model with getter-like methods
    class Model {
        int getX1() { return 1; }
        int getX2() { return 1; }
        int getX3() { return 1; }
        int getX4() { return 1; }
        int getX5() { return 1; }
        int getX6() { return 1; }
        int getX7() { return 1; }
        int getX8() { return 1; }
        int getX9() { return 1; }
        boolean isOk() { return true; }
        boolean hasReadyFlag() { return true; }
        String getName() { return "Alice"; }
    }

    // main is optional; helps if someone runs it normally
    public static void main(String[] args) {
        TestHollywoodPrinciple t = new TestHollywoodPrinciple();
        t.new Orchestrator().orchestrate();
        t.new LowComplexity().doWork();
        t.new GetterHeavyOnly().getterHeavy();
    }
}
