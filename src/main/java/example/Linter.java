package example;

public interface Linter {
    void lintClass();

    LinterType getType();

    default boolean isPerClass() {
        return true;
    }
}