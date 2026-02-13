package example;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Test class for LawOfDemeterPrinciple linter.
 */
public class TestLawOfDemeter {
    private House house = new House();

    // VIOLATION: chainDepth > 1 (house.getKitchen().getToaster().toast())
    public void violationMethod() {
        house.getKitchen().getToaster().toast();
    }

    // EXCLUDED: Should be ignored because of "stream", "map", "collect" patterns
    public void streamExclusionMethod() {
        List<String> items = new ArrayList<>();
        items.stream()
             .map(String::toLowerCase)
             .collect(Collectors.toList());
    }

    // EXCLUDED: Should be ignored because of "Builder" pattern
    public void builderExclusionMethod() {
        new UserBuilder()
                .setName("Alice")
                .setAge(30)
                .build();
    }

    // EXCLUDED: Should be ignored because of "java/lang/String"
    public void stringExclusionMethod() {
        String test = "  hello  ";
        int length = test.trim().toLowerCase().length();
    }

    // VALID: Only one level of nesting (calling a method on a field)
    public void validMethod() {
        house.clean();
    }

    // Helper classes for testing
    class House {
        Kitchen kitchen = new Kitchen();
        Kitchen getKitchen() { return kitchen; }
        void clean() {}
    }

    class Kitchen {
        Toaster toaster = new Toaster();
        Toaster getToaster() { return toaster; }
    }

    class Toaster {
        void toast() {}
    }

    class UserBuilder {
        public UserBuilder setName(String name) { return this; }
        public UserBuilder setAge(int age) { return this; }
        public String build() { return "User"; }
    }
}