package testclasses.example;

public class TestDependencyInversionPrinciple {

    interface Repository {
        void save(String data);

        String findById(String id);
    }

    interface Notifier {
        void send(String message);
    }

    static abstract class AbstractValidator {
        public abstract boolean isValid(String input);
    }

    static class DatabaseRepository implements Repository {
        public void save(String data) {

        }

        public String findById(String id) {
            return "";
        }
    }

    static class EmailNotifier implements Notifier {
        public void send(String message) {

        }
    }

    static class RegexValidator extends AbstractValidator {
        public boolean isValid(String input) {
            return input != null && !input.isEmpty();
        }
    }

    static class GoodUserService {
        private Repository repo;
        private Notifier notifier;

        public GoodUserService(Repository repo, Notifier notifier) {
            this.repo = repo;
            this.notifier = notifier;
        }

        public void createUser(String id) {
            repo.save(id);
            notifier.send("User created: " + id);
        }
    }

    abstract static class AbstractService {
        protected Repository repo;

        public AbstractService(Repository repo) {
            this.repo = repo;
        }

        public abstract void execute(String id);
    }

    static class BadFieldService {
        private DatabaseRepository repo;

        public void createUser(String id) {
            repo.save(id);
        }
    }

    static class BadParamService {
        private Repository repo;

        public BadParamService(Repository repo) {
            this.repo = repo;
        }

        public void notifyWith(EmailNotifier notifier) {
            notifier.send("Hello from " + repo.findById("1"));
        }
    }

    static class BadInstantiationService {
        private Repository repo;

        public BadInstantiationService() {
            this.repo = new DatabaseRepository();
        }

        public void createUser(String id) {
            repo.save(id);
        }
    }

    static class BadDoubleViolation {
        private DatabaseRepository repo = new DatabaseRepository();

        public void process(String id) {
            repo.save(id);
        }
    }
}
