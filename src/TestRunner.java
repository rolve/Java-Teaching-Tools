import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.HashSet;
import java.util.InputMismatchException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.PatternSyntaxException;

import org.junit.ComparisonFailure;
import org.junit.internal.ArrayComparisonFailure;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runners.model.TestTimedOutException;

public class TestRunner {
    
    static final Set<Class<? extends Throwable>> junitExceptions = new HashSet<>(asList(
            ComparisonFailure.class, ArrayComparisonFailure.class,
            AssertionError.class, TestTimedOutException.class));
    
    static final Set<Class<? extends Throwable>> knownExceptions = new HashSet<>(asList(
            StackOverflowError.class, OutOfMemoryError.class, NullPointerException.class,
            ArrayIndexOutOfBoundsException.class, StringIndexOutOfBoundsException.class,
            IndexOutOfBoundsException.class, InputMismatchException.class,
            NoSuchElementException.class, FileNotFoundException.class,
            IllegalArgumentException.class, NumberFormatException.class,
            ArithmeticException.class, EmptyStackException.class, 
            PatternSyntaxException.class, IllegalStateException.class));

    private static final int REPETITIONS = 7;

    // For how long we do REPETITIONS. If we were running tests for more than this time, do not
    // attempt another repetition.
    private static final long MAX_RUNNING_TIME = 10000; // millisecs
    
    public static void main(String[] args) throws ClassNotFoundException, IOException {
        String testClass = args[0];
        Set<String> classes = stream(args).skip(1).collect(toSet());
        
        new TestRunner(testClass, classes).runTests();
    }

    private String testClass;
    private Set<String> classes;
    
    public TestRunner(String testClass, Set<String> classes) {
        this.testClass = testClass;
        this.classes = classes;
    }

    private void runTests() throws IOException, ClassNotFoundException {
        // Close standard input in case some solutions read from it
        System.in.close();
        
        PrintStream stdOut = System.out;
        PrintStream stdErr = System.err;
        System.setOut(new PrintStream(new OutputStream() {
            public void write(int b) {}
        }));
        System.setErr(new PrintStream(new OutputStream() {
            public void write(int b) {}
        }));
        
        List<Set<String>> succeededTests = new ArrayList<>();
        Set<String> failedTests = new HashSet<>();
        SortedSet<String> failures = new TreeSet<>();
        
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < REPETITIONS; i++) {
            Set<String> all = new HashSet<>();
            Set<String> failed = new HashSet<>();
            JUnitCore core = new JUnitCore();
            core.addListener(new RunListener() {
                public void testFinished(Description description) {
                    all.add(description.getMethodName());
                }
                public void testFailure(Failure failure) throws Exception {
                    String name = failure.getDescription().getMethodName();
                    failed.add(name);
                    failedTests.add(name);

                    String msg = failure.toString();
                    Throwable exception = failure.getException();
                    if (dontPrintTrace(exception)) {
                        msg += " (" + exception.getClass().getName() + ")";
                    } else {
                        msg += "\n" + failure.getTrace();
                    }
                    failures.add(msg);
                }
            });
            core.run(Class.forName(testClass));
            all.removeAll(failed);
            succeededTests.add(all);
            
            if (i < REPETITIONS - 1 && System.currentTimeMillis() - startTime > MAX_RUNNING_TIME) {
                // this timeout is not so bad. It just means that we are not pretty sure that the
                // tests are deterministic, since not all REPETITIONS were tried
                stdOut.println("SOFT_TIMEOUT");
                stdErr.println("SOFT_TIMEOUT");
                break;
            }
        }

        failures.stream()
                .map(s -> stream(s.split("\n")).map(l -> "    " + l).collect(joining("\n")))
                .forEach(stdErr::println);
        
        List<Set<String>> different = succeededTests.stream().distinct().collect(toList());
        if (different.size() > 1) {
            Set<String> deterministic = new HashSet<>(different.get(0));
            different.forEach(deterministic::retainAll);
            
            Set<String> nonDeterm = new HashSet<>(failedTests);
            nonDeterm.removeAll(deterministic);
            
            stdErr.println("Non-determinism detected in tests: " + nonDeterm);
            stdOut.println("nondeterministic");
        }
        
        Set<String> alwaysSucc = new HashSet<>(different.get(0));
        alwaysSucc.removeAll(failedTests);
        
        alwaysSucc.forEach(stdOut::println);
        stdOut.flush();
        stdErr.flush();
    }
    
    private boolean dontPrintTrace(Throwable exception) {
        Class<? extends Throwable> clazz = exception.getClass();
        
        boolean inCodeUnderTest = stream(exception.getStackTrace())
                .map(StackTraceElement::getClassName)
                .anyMatch(classes::contains);
        
        return inCodeUnderTest && knownExceptions.contains(clazz) ||
                junitExceptions.contains(clazz);
    }
}
