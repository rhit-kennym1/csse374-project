package testclasses.example;

/**
 * SHOULD TRIGGER TemporalCouplingLinter:
 * - Calls execute()/process() on a local receiver without any prior init/open/start/etc.
 */
public class TestTemporalCouplingViolation {

    static class Worker {
        void init() { /* no-op */ }
        void execute() { /* no-op */ }
    }

    static class Worker2 {
        void init() { /* no-op */ }
        void process() { /* no-op */ }
    }

    public void violatesTemporalCoupling() {
        Worker w = new Worker();
        // No init/open/start call before execute -> should be flagged
        w.execute();
    }

    // Another "use" call name that should also be flagged
    public void violatesWithProcess() {
        Worker2 w = new Worker2();
        // No init/open/start call before process -> should be flagged
        w.process();
    }
}
