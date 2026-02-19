package testclasses.example;

/**
 * Test cases for cyclic dependency detection
 */
public class TestCyclicDependencies {

    // ========== SIMPLE CYCLE: A -> B -> A ==========
    
    static class ServiceA {
        private ServiceB serviceB;
        
        public void doSomething() {
            serviceB.process();
        }
        
        public void handleRequest() {
            System.out.println("ServiceA handling");
        }
    }
    
    static class ServiceB {
        private ServiceA serviceA;
        
        public void process() {
            serviceA.handleRequest();
        }
        
        public void execute() {
            System.out.println("ServiceB executing");
        }
    }

    // ========== THREE-WAY CYCLE: X -> Y -> Z -> X ==========
    
    static class ComponentX {
        private ComponentY componentY;
        
        public void methodX() {
            componentY.methodY();
        }
    }
    
    static class ComponentY {
        private ComponentZ componentZ;
        
        public void methodY() {
            componentZ.methodZ();
        }
    }
    
    static class ComponentZ {
        private ComponentX componentX;
        
        public void methodZ() {
            componentX.methodX();
        }
    }

    // ========== COMPLEX CYCLE: Multiple interconnected cycles ==========
    
    static class ManagerA {
        private ManagerB managerB;
        private ManagerC managerC;
        
        public void coordinate() {
            managerB.delegate();
            managerC.execute();
        }
    }
    
    static class ManagerB {
        private ManagerC managerC;
        
        public void delegate() {
            managerC.execute();
        }
    }
    
    static class ManagerC {
        private ManagerA managerA;
        
        public void execute() {
            managerA.coordinate();
        }
    }

    // ========== NO CYCLE: Linear dependency chain ==========
    
    static class LayerOne {
        private LayerTwo layerTwo;
        
        public void process() {
            layerTwo.handle();
        }
    }
    
    static class LayerTwo {
        private LayerThree layerThree;
        
        public void handle() {
            layerThree.execute();
        }
    }
    
    static class LayerThree {
        // No dependencies - end of chain
        
        public void execute() {
            System.out.println("Final layer");
        }
    }

    // ========== INDEPENDENT CLASSES: No cycles ==========
    
    static class IndependentA {
        public void doWork() {
            System.out.println("Independent A");
        }
    }
    
    static class IndependentB {
        public void doWork() {
            System.out.println("Independent B");
        }
    }

    // ========== SELF-REFERENCING (typically OK, not a multi-class cycle) ==========
    
    static class Node {
        private Node next;
        private Node previous;
        
        public void link(Node other) {
            this.next = other;
            other.previous = this;
        }
    }

    // ========== COMPLEX SCENARIO: Mix of cycles and non-cycles ==========
    
    static class OrderService {
        private InventoryService inventory;
        private PaymentService payment;
        
        public void placeOrder() {
            inventory.checkStock();
            payment.processPayment();
        }
    }
    
    static class InventoryService {
        private OrderService orderService;  // Creates cycle with OrderService
        
        public void checkStock() {
            System.out.println("Checking stock");
        }
        
        public void restock() {
            orderService.placeOrder();  // Cycle!
        }
    }
    
    static class PaymentService {
        // No cycle - only depends on external services
        
        public void processPayment() {
            System.out.println("Processing payment");
        }
    }

    // ========== BIDIRECTIONAL DEPENDENCY (cycle between 2 classes) ==========
    
    static class Controller {
        private View view;
        
        public void updateView() {
            view.render();
        }
        
        public void handleEvent() {
            System.out.println("Event handled");
        }
    }
    
    static class View {
        private Controller controller;
        
        public void render() {
            System.out.println("Rendering");
        }
        
        public void userAction() {
            controller.handleEvent();
        }
    }
}