package testclasses.example;

public class TestDecoratorPattern {

    // Valid Decorator Example
    private Component component = new ConcreteComponent();

    public void validDecoratorExample() {
        ValidDecorator decorator = new ValidDecorator(component);
        decorator.execute();
    }

    // Bad Decorator Examples
    public void badPublicFieldExample() {
        Beverage beverage = new SimpleBeverage();
        BadDecoratorPublic decorator = new BadDecoratorPublic(beverage);
        // Can access: decorator.beverage (violation!)
    }

    public void badNonFinalExample() {
        Reader reader = new FileReader();
        BadDecoratorNonFinal decorator = new BadDecoratorNonFinal(reader);
        decorator.setReader(new FileReader()); // Can change (violation!)
    }

    public void badNoConstructorExample() {
        BadDecoratorNoConstructor decorator = new BadDecoratorNoConstructor();
        // No way to inject component (violation!)
    }

    public void badWrongInterfaceExample() {
        Input input = new FileInput();
        BadDecoratorWrongInterface decorator = new BadDecoratorWrongInterface(input);
        // Implements different interface (violation!)
    }

    // Valid Decorator Pattern - PASS
    interface Component {
        void execute();
    }

    class ConcreteComponent implements Component {
        public void execute() {
            System.out.println("Executing");
        }
    }

    class ValidDecorator implements Component {
        private final Component component;

        public ValidDecorator(Component component) {
            this.component = component;
        }

        public void execute() {
            component.execute();
        }
    }

    // Bad: Public Field - FAIL
    interface Beverage {
        String getDescription();
    }

    class SimpleBeverage implements Beverage {
        public String getDescription() {
            return "Simple";
        }
    }

    class BadDecoratorPublic implements Beverage {
        public final Beverage beverage;  // PUBLIC - violation!

        public BadDecoratorPublic(Beverage beverage) {
            this.beverage = beverage;
        }

        public String getDescription() {
            return beverage.getDescription();
        }
    }

    // Bad: Non-Final Field - FAIL
    interface Reader {
        String read();
    }

    class FileReader implements Reader {
        public String read() {
            return "data";
        }
    }

    class BadDecoratorNonFinal implements Reader {
        private Reader reader;  // NOT FINAL - violation!

        public BadDecoratorNonFinal(Reader reader) {
            this.reader = reader;
        }

        public void setReader(Reader reader) {
            this.reader = reader;
        }

        public String read() {
            return reader.read();
        }
    }

    // Bad: No Constructor - FAIL
    interface DataSource {
        void write(String data);
    }

    class FileDataSource implements DataSource {
        public void write(String data) {
            System.out.println(data);
        }
    }

    class BadDecoratorNoConstructor implements DataSource {
        private final DataSource source;

        public BadDecoratorNoConstructor() {  // NO PARAMETER - violation!
            this.source = new FileDataSource();
        }

        public void write(String data) {
            source.write(data);
        }
    }

    // Bad: Wrong Interface - FAIL
    interface Input {
        String read();
    }

    interface Output {
        void write(String data);
    }

    class FileInput implements Input {
        public String read() {
            return "data";
        }
    }

    class BadDecoratorWrongInterface implements Output {
        private final Input input;  // Different interface - violation!

        public BadDecoratorWrongInterface(Input input) {
            this.input = input;
        }

        public void write(String data) {
            System.out.println(data);
        }
    }
}