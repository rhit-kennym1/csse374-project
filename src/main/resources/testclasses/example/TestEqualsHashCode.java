package example;

public class TestEqualsHashCode {

    public String name;
    public int value;

    // Has equals but no hashCode - EqualsHashCodeLinter will catch this
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        TestEqualsHashCode other = (TestEqualsHashCode) obj;
        return value == other.value;
    }

    // No hashCode method - violation!

    public void doSomething() {
        System.out.println("Doing something");
    }
}