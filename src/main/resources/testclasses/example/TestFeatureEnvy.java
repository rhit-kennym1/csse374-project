package testclasses.example;

public class TestFeatureEnvy {

    private Customer customer = new Customer();
    private Order order = new Order();

    public void validMethodExample() {
        String name = "Test";
        int value = 42;
        processLocal(name, value);
    }

    private void processLocal(String name, int value) {
        System.out.println(name + value);
    }

    public void featureEnvyExample() {
        String name = customer.getName();
        String address = customer.getAddress();
        String phone = customer.getPhone();
        String email = customer.getEmail();
        int age = customer.getAge();
    }

    public void orderEnvyExample() {
        double total = order.getTotal();
        String status = order.getStatus();
        int itemCount = order.getItemCount();
        String shippingAddress = order.getShippingAddress();
    }

    public void mixedAccessExample() {
        String localData = "data";
        customer.getName();
        processLocal(localData, 1);
        customer.getAddress();
    }

    class Customer {
        private String name;
        private String address;
        private String phone;
        private String email;
        private int age;

        public String getName() {
            return name;
        }

        public String getAddress() {
            return address;
        }

        public String getPhone() {
            return phone;
        }

        public String getEmail() {
            return email;
        }

        public int getAge() {
            return age;
        }
    }

    class Order {
        private double total;
        private String status;
        private int itemCount;
        private String shippingAddress;

        public double getTotal() {
            return total;
        }

        public String getStatus() {
            return status;
        }

        public int getItemCount() {
            return itemCount;
        }

        public String getShippingAddress() {
            return shippingAddress;
        }
    }
}