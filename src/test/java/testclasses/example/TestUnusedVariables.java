package testclasses.example;

import java.util.ArrayList;
import java.util.List;

public class TestUnusedVariables {



    // SHOULD NOT TRIGGER
    private int usedPrivateField = 42;

    // SHOULD NOT TRIGGER
    private int writeOnlyPrivateField;

    // SHOULD NOT TRIGGER
    private final List<String> names;

    public TestUnusedVariables() {
        names = new ArrayList<>();
        names.add("Alice");

        writeOnlyPrivateField = 777;

        int unusedLocalInCtor = 999;

        int usedLocalInCtor = 10;
        System.out.println("usedLocalInCtor=" + usedLocalInCtor);
    }

    public void validMethod() {
        System.out.println("usedPrivateField=" + usedPrivateField);
        System.out.println("names size=" + names.size());

        // SHOULD NOT TRIGGER
        int usedLocal = 5;
        System.out.println(usedLocal);

        // SHOULD NOT TRIGGER
        if (usedLocal > 0) {
            int branchUsed = usedLocal + 1;
            System.out.println(branchUsed);
        }

    }

    public void callsPrivateMethod() {
        usedPrivateMethod();
    }

    // SHOULD NOT TRIGGER
    private void usedPrivateMethod() {
        System.out.println("I am called");
    }


    public static void main(String[] args) {
        TestUnusedVariables t = new TestUnusedVariables();
        t.validMethod();
        t.callsPrivateMethod();
    }
}
