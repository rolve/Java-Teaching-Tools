package ch.trick17.jtt.sandbox;

public class SandboxResult<T> {

    public enum Kind {
        NORMAL, EXCEPTION, TIMEOUT, ILLEGAL_OPERATION;
    }

    public static <T> SandboxResult<T> normal(T value) {
        return new SandboxResult<>(Kind.NORMAL, value, null);
    }

    public static <T> SandboxResult<T> exception(Throwable exception) {
        return new SandboxResult<>(Kind.EXCEPTION, null, exception);
    }

    public static <T> SandboxResult<T> timeout() {
        return new SandboxResult<>(Kind.TIMEOUT, null, null);
    }

    public static <T> SandboxResult<T> illegalOperation(Throwable exception) {
        return new SandboxResult<>(Kind.ILLEGAL_OPERATION, null, exception);
    }

    private final Kind kind;
    private final T value;
    private final Throwable exception;

    private SandboxResult(Kind kind, T value, Throwable exception) {
        this.kind = kind;
        this.value = value;
        this.exception = exception;
    }

    public Kind kind() {
        return kind;
    }

    public T value() {
        if (kind != Kind.NORMAL) {
            throw new IllegalStateException();
        }
        return value;
    }

    public Throwable exception() {
        if (kind != Kind.EXCEPTION && kind != Kind.ILLEGAL_OPERATION) {
            throw new IllegalStateException();
        }
        return exception;
    }
}
