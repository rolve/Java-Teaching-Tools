package ch.trick17.jtt.grader.test;

import ch.trick17.jtt.grader.test.TestResults.MethodResult;
import ch.trick17.jtt.sandbox.CustomCxtClassLoaderRunner;
import ch.trick17.jtt.sandbox.JavaSandbox;
import ch.trick17.jtt.sandbox.SandboxResult;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.*;

import static ch.trick17.jtt.junitextensions.internal.ScoreExtension.SCORE_KEY;
import static ch.trick17.jtt.sandbox.InputMode.EMPTY;
import static ch.trick17.jtt.sandbox.OutputMode.DISCARD;
import static ch.trick17.jtt.sandbox.SandboxResult.Kind.*;
import static java.io.File.pathSeparator;
import static java.lang.Double.parseDouble;
import static java.lang.String.valueOf;
import static java.lang.System.currentTimeMillis;
import static java.lang.System.getProperty;
import static java.lang.Thread.currentThread;
import static java.util.Arrays.stream;
import static java.util.List.copyOf;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.junit.platform.engine.TestDescriptor.Type.TEST;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

public class TestExecutor {

    public static TestResults execute(TestRunConfig config) {
        var methodResults = new ArrayList<MethodResult>();
        for (var method : findTestMethods(config)) {
            var startTime = currentTimeMillis();

            var passed = false;
            var failed = false;
            var failMsgs = new LinkedHashSet<String>();
            var repsMade = config.repetitions();
            var timeout = false;
            var outOfMemory = false;
            var illegalOps = new ArrayList<String>();
            var scores = new ArrayList<Double>();
            for (int rep = 1; rep <= config.repetitions(); rep++) {
                var methodResult = runSandboxed(method, config);

                if (methodResult.kind() == TIMEOUT) {
                    timeout = true;
                } else if (methodResult.kind() == OUT_OF_MEMORY) {
                    outOfMemory = true;
                } else if (methodResult.kind() == ILLEGAL_OPERATION) {
                    illegalOps.add(methodResult.exception().getMessage());
                } else if (methodResult.kind() == EXCEPTION) {
                    // should not happen, JUnit catches exceptions
                    throw new AssertionError(methodResult.exception());
                } else {
                    var junitResult = methodResult.value();
                    if (junitResult.get("throwable") == null) {
                        passed = true;
                    } else {
                        failed = true;
                        var throwable = (Throwable) junitResult.get("throwable");
                        var msg = valueOf(throwable.getMessage()).replaceAll("\\s+", " ")
                                + " (" + throwable.getClass().getName() + ")";
                        // TODO: Collect exception stats
                        failMsgs.add(msg);
                    }
                    if (junitResult.get("score") != null) {
                        scores.add((Double) junitResult.get("score"));
                    }
                }

                if (rep < config.repetitions() &&
                        currentTimeMillis() - startTime > config.testTimeout().toMillis()) {
                    repsMade = rep;
                    break;
                }
            }

            var nonDeterm = passed && failed;
            passed &= !nonDeterm;

            var name = method.getMethodName();
            if (method.getClassName().contains("$")) { // nested test
                var prefix = stream(method.getClassName().split("\\$"))
                        .skip(1) // skip outermost class
                        .collect(joining("."));
                name = prefix + "." + name;
            }
            var incompleteReps = repsMade < config.repetitions();
            methodResults.add(new MethodResult(name, passed, copyOf(failMsgs), nonDeterm,
                    repsMade, incompleteReps, timeout, outOfMemory, illegalOps, scores));
        }

        return new TestResults(methodResults);
    }

    private static List<MethodSource> findTestMethods(TestRunConfig config) {
        var urls = new ArrayList<URL>();
        urls.add(toUrl(config.codeUnderTest()));
        urls.add(toUrl(config.testCode()));
        config.dependencies().forEach(p -> urls.add(toUrl(p)));
        urls.addAll(currentClassPath());

        // to discover test classes, JUnit needs to *load* them, so we create
        // a custom class loader with a classpath that includes the test code
        // and set it as the "context class loader" of the current thread
        var loader = new URLClassLoader(urls.toArray(URL[]::new),
                currentThread().getContextClassLoader());
        return new CustomCxtClassLoaderRunner(loader).run(() -> {
            var launcher = LauncherFactory.create();
            var classReq = request()
                    .configurationParameter("junit.jupiter.testmethod.order.default",
                            "org.junit.jupiter.api.MethodOrderer$DisplayName")
                    .selectors(selectClass(config.testClassName()));
            var testPlan = launcher.discover(classReq.build());
            return testPlan.getRoots().stream()
                    .flatMap(id -> testPlan.getDescendants(id).stream())
                    .filter(id -> id.getType() == TEST)
                    .map(id -> (MethodSource) id.getSource().orElseThrow())
                    .collect(toList());
        });
    }

    private static List<URL> currentClassPath() {
        return stream(getProperty("java.class.path").split(pathSeparator))
                .map(path -> toUrl(Path.of(path)))
                .collect(toList());
    }

    @SuppressWarnings("unchecked")
    private static SandboxResult<Map<String, Object>> runSandboxed(MethodSource test, TestRunConfig config) {
        var restricted = List.of(toUrl(config.codeUnderTest()));
        var unrestricted = new ArrayList<URL>();
        unrestricted.add(toUrl(config.testCode()));
        config.dependencies().forEach(p -> unrestricted.add(toUrl(p)));
        unrestricted.addAll(currentClassPath());

        var sandbox = new JavaSandbox()
                .permRestrictions(config.permRestrictions())
                .timeout(config.repTimeout())
                .stdInMode(EMPTY).stdOutMode(DISCARD).stdErrMode(DISCARD);
        var args = List.of(test.getClassName(), test.getMethodName());
        var result = sandbox.run(restricted, unrestricted, Sandboxed.class,
                "run", List.of(String.class, String.class), args, Map.class);
        return (SandboxResult<Map<String, Object>>) (Object) result;
    }

    public static class Sandboxed {
        public static Map<String, Object> run(String className, String methodName) {
            var sel = selectMethod(className, methodName);
            var req = request().selectors(sel).build();
            var listener = new TestExecutionListener() {
                TestExecutionResult result;
                Double score;

                public void reportingEntryPublished(TestIdentifier id, ReportEntry entry) {
                    entry.getKeyValuePairs().entrySet().stream()
                            .filter(e -> e.getKey().equals(SCORE_KEY))
                            .map(e -> parseDouble(e.getValue()))
                            .findFirst()
                            .ifPresent(s -> score = s);
                }

                public void executionFinished(TestIdentifier id, TestExecutionResult res) {
                    if (id.isTest()) {
                        result = res;
                    }
                }
            };
            LauncherFactory.create().execute(req, listener);

            var throwable = listener.result == null
                    ? null // test was skipped
                    : listener.result.getThrowable().orElse(null);

            // since JUnit catches the SecurityException, need to rethrow it
            // for the sandbox to record the illegal operation...
            if (throwable instanceof SecurityException) {
                throw (SecurityException) throwable;
            }

            // can only transfer classes loaded by the bootstrap class loader
            // across sandbox boundary...
            var result = new HashMap<String, Object>();
            result.put("throwable", throwable);
            result.put("score", listener.score);
            return result;
        }
    }

    private static URL toUrl(Path p) {
        try {
            return p.toUri().toURL();
        } catch (MalformedURLException e) {
            throw new AssertionError(e);
        }
    }
}
