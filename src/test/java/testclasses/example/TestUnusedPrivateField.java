package testclasses.example;

public class TestUnusedPrivateField {

    // This field is never read or written anywhere.
    // No initializer → no PUTFIELD in constructor.
    private int trulyUnusedField;

    public void doNothing() {
        // intentionally empty
    }
}
