package testclasses.example;

public class TestMissingImplementation {

    // PartialImpl.class (stale) only has process(); misses validate()
    interface Processable {
        void validate();
    }

    // LazyWorker.class (stale) only has doWork() + cleanup(); misses shutdown()
    abstract static class AbstractWorker {
        public abstract void doWork();

        public abstract void cleanup();

        public abstract void shutdown();
    }

    // IncompleteReporter.class (stale) only has generate() + send() + archive(); misses audit()
    interface Reportable {
        void generate();

        void send();

        void archive();

        void audit();
    }
}
