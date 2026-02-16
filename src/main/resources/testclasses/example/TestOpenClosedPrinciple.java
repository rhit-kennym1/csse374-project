package testclasses.example;

public class TestOpenClosedPrinciple {

    // Good OCP Example
    private PaymentProcessor processor = new CreditCardProcessor("1234");

    public void goodOCPExample() {
        processor.process(100.0);
        processor.authorize();
    }

    // Bad OCP Examples
    public void badPublicFieldsExample() {
        Account account = new Account("123", "John");
        account.balance = 1000.0;  // Direct access - violation!
    }

    public void badNoInterfaceExample() {
        DataManager manager = new DataManager();
        manager.method1();
        manager.method2();
        // Many methods, no interface - violation!
    }

    public void badShouldBeFinalExample() {
        Calculator calc = new Calculator(10);
        calc.calculate();
        // Can be extended unintentionally - violation!
    }

    // Good: Implements Interface - PASS
    interface PaymentProcessor {
        void process(double amount);
        void refund(double amount);
        String getStatus();
        void cancel();
        void verify();
        void authorize();
    }

    class CreditCardProcessor implements PaymentProcessor {
        private String status;
        private final String cardNumber;

        public CreditCardProcessor(String cardNumber) {
            this.cardNumber = cardNumber;
            this.status = "READY";
        }

        public void process(double amount) {
            status = "PROCESSING";
        }

        public void refund(double amount) {
            status = "REFUNDING";
        }

        public String getStatus() {
            return status;
        }

        public void cancel() {
            status = "CANCELLED";
        }

        public void verify() {
            status = "VERIFIED";
        }

        public void authorize() {
            status = "AUTHORIZED";
        }
    }

    // Bad: Public Mutable Fields - FAIL
    class Account {
        public String accountNumber;  // PUBLIC MUTABLE - violation!
        public double balance;        // PUBLIC MUTABLE - violation!
        public String owner;          // PUBLIC MUTABLE - violation!

        public static final int MAX_WITHDRAWAL = 1000;

        public Account(String accountNumber, String owner) {
            this.accountNumber = accountNumber;
            this.owner = owner;
            this.balance = 0.0;
        }

        public void deposit(double amount) {
            balance += amount;
        }
    }

    // Bad: No Interface with Many Methods - FAIL
    class DataManager {
        private String data;

        public DataManager() {
            this.data = "";
        }

        public void method1() {
            data += "1";
        }

        public void method2() {
            data += "2";
        }

        public void method3() {
            data += "3";
        }

        public void method4() {
            data += "4";
        }

        public void method5() {
            data += "5";
        }

        public void method6() {
            data += "6";
        }

        public void method7() {
            data += "7";
        }

        public void method8() {
            data += "8";
        }
    }

    // Bad: Should Be Final - FAIL
    class Calculator {
        private int value;

        public Calculator(int value) {
            this.value = value;
        }

        public void calculate() {
            value *= 2;
        }

        public void process() {
            value += 10;
        }

        public void update() {
            value--;
        }

        public int getValue() {
            return value;
        }
    }

    // Bad: Abstract with Too Much Concrete - FAIL
    abstract class BaseProcessor {
        protected String state;

        public BaseProcessor() {
            this.state = "READY";
        }

        public abstract void process();

        public void method1() {
            state = "1";
        }

        public void method2() {
            state = "2";
        }

        public void method3() {
            state = "3";
        }

        public void method4() {
            state = "4";
        }

        public void method5() {
            state = "5";
        }

        public void method6() {
            state = "6";
        }

        public void method7() {
            state = "7";
        }

        public void method8() {
            state = "8";
        }

        public String getState() {
            return state;
        }
    }
}