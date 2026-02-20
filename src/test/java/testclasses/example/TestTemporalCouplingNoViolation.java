package testclasses.example;

/**
 * SHOULD NOT TRIGGER TemporalCouplingLinter:
 * - Calls init()/open() before execute()/close() on the same local receiver.
 */
public class TestTemporalCouplingNoViolation {

    static class Worker {
        void init() { /* no-op */ }
        void execute() { /* no-op */ }
    }

    public void okTemporalCoupling() {
        Worker w = new Worker();
        w.init();      // setup recognized
        w.execute();   // should NOT be flagged
    }

    // Also ok: open() then close()
    static class Resource {
        void open() { /* no-op */ }
        void close() { /* no-op */ }
    }

    public void okOpenClose() {
        Resource r = new Resource();
        r.open();
        r.close(); // should NOT be flagged because setup seen for same var
    }
}
