package ch.trick17.jtt.grader;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Set;

import static ch.trick17.jtt.grader.Compiler.ECLIPSE;
import static ch.trick17.jtt.grader.Compiler.JAVAC;
import static ch.trick17.jtt.sandbox.Whitelist.DEFAULT_WHITELIST_DEF;
import static java.nio.file.Files.list;
import static java.nio.file.Files.readAllLines;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.*;

public class GraderTest {

    static final Path TEST_SRC_DIR = Path.of("tests");
    static final Path SUBM_ROOT = Path.of("test-submissions");
    static final Codebase ECLIPSE_BASE = new MultiCodebase(
            SUBM_ROOT.resolve("eclipse-structure"), ProjectStructure.ECLIPSE);
    static final Codebase MVN_BASE = new MultiCodebase(
            SUBM_ROOT.resolve("maven-structure"), ProjectStructure.MAVEN);

    static final List<String> EXPECTED_ADD_SIMPLE_EC = List.of(
            "Name\tcompiled\tcompile errors\ttestAdd1\ttestAdd2",
            "compile-error\t1\t1\t0\t0",
            "correct\t1\t0\t1\t1",
            "fails-test\t1\t0\t1\t0");
    static final List<String> EXPECTED_ADD_SIMPLE_JC = List.of(
            "Name\tcompiled\tcompile errors\ttest compile errors\ttestAdd1\ttestAdd2",
            "compile-error\t0\t1\t1\t0\t0",
            "correct\t1\t0\t0\t1\t1",
            "fails-test\t1\t0\t0\t1\t0");

    private static Grader grader;

    @BeforeAll
    public static void setUp() {
        grader = new Grader();
        grader.setLogDir(null);
    }

    @Test
    public void testResults() throws IOException {
        var tasks = List.of(Task.fromClassName("AddTest", TEST_SRC_DIR).compiler(JAVAC));
        grader.gradeOnly("correct", "fails-test", "compile-error");
        var resultsList = grader.run(ECLIPSE_BASE, tasks);

        assertEquals(1, resultsList.size());
        var results = resultsList.get(0);
        assertEquals(3, results.submissionResults().size());

        assertTrue(results.get("correct").testResults().methodResults().get(0).passed());
        assertTrue(results.get("correct").testResults().methodResults().get(1).passed());

        assertTrue(results.get("fails-test").testResults().methodResults().get(0).passed());
        assertFalse(results.get("fails-test").testResults().methodResults().get(1).passed());

        assertTrue(results.get("compile-error").compileErrors());
        assertFalse(results.get("compile-error").compiled());
        assertNull(results.get("compile-error").testResults());
    }

    @Test
    public void testEclipseStructureEclipseCompiler() throws IOException {
        var tasks = List.of(Task.fromClassName("AddTest", TEST_SRC_DIR).compiler(ECLIPSE));
        grader.gradeOnly("correct", "fails-test", "compile-error");
        grader.run(ECLIPSE_BASE, tasks);
        var results = readAllLines(Path.of("results-AddTest.tsv"));
        assertEquals(EXPECTED_ADD_SIMPLE_EC, results);
    }

    @Test
    public void testEclipseStructureJavac() throws IOException {
        var tasks = List.of(Task.fromClassName("AddTest", TEST_SRC_DIR).compiler(JAVAC));
        grader.gradeOnly("correct", "fails-test", "compile-error");
        grader.run(ECLIPSE_BASE, tasks);
        var results = readAllLines(Path.of("results-AddTest.tsv"));
        assertEquals(EXPECTED_ADD_SIMPLE_JC, results);
    }

    @Test
    public void testMavenStructureEclipseCompiler() throws IOException {
        var tasks = List.of(Task.fromClassName("AddTest", TEST_SRC_DIR).compiler(ECLIPSE));
        grader.gradeOnly("correct", "fails-test", "compile-error");
        grader.run(MVN_BASE, tasks);
        var results = readAllLines(Path.of("results-AddTest.tsv"));
        assertEquals(EXPECTED_ADD_SIMPLE_EC, results);
    }

    @Test
    public void testMavenStructureJavac() throws IOException {
        var tasks = List.of(Task.fromClassName("AddTest", TEST_SRC_DIR).compiler(JAVAC));
        grader.gradeOnly("correct", "fails-test", "compile-error");
        grader.run(MVN_BASE, tasks);
        var results = readAllLines(Path.of("results-AddTest.tsv"));
        assertEquals(EXPECTED_ADD_SIMPLE_JC, results);
    }

    @Test
    public void testPackageEclipseCompiler() throws IOException {
        var tasks = List.of(Task.fromClassName("multiply.MultiplyTest", TEST_SRC_DIR).compiler(ECLIPSE));
        grader.gradeOnly("correct", "fails-test", "compile-error");
        grader.run(ECLIPSE_BASE, tasks);
        var results = readAllLines(Path.of("results-MultiplyTest.tsv"));
        var expected = List.of(
                "Name\tcompiled\tcompile errors\ttestMultiply1\ttestMultiply2",
                "compile-error\t1\t1\t0\t0",
                "correct\t1\t0\t1\t1",
                "fails-test\t1\t0\t1\t0");
        assertEquals(expected, results);
    }

    @Test
    public void testPackageJavac() throws IOException {
        var tasks = List.of(Task.fromClassName("multiply.MultiplyTest", TEST_SRC_DIR).compiler(JAVAC));
        grader.gradeOnly("correct", "fails-test", "compile-error");
        grader.run(ECLIPSE_BASE, tasks);
        var results = readAllLines(Path.of("results-MultiplyTest.tsv"));
        var expected = List.of(
                "Name\tcompiled\tcompile errors\ttest compile errors\ttestMultiply1\ttestMultiply2",
                "compile-error\t0\t1\t1\t0\t0",
                "correct\t1\t0\t0\t1\t1",
                "fails-test\t1\t0\t0\t1\t0");
        assertEquals(expected, results);
    }

    @Test
    public void testUnrelatedCompileErrorEclipseCompiler() throws IOException {
        var tasks = List.of(Task.fromClassName("AddTest", TEST_SRC_DIR).compiler(ECLIPSE));
        grader.gradeOnly("correct", "unrelated-compile-error");
        grader.run(ECLIPSE_BASE, tasks);
        var results = readAllLines(Path.of("results-AddTest.tsv"));
        var expected = List.of(
                "Name\tcompiled\tcompile errors\ttestAdd1\ttestAdd2",
                "correct\t1\t0\t1\t1",
                "unrelated-compile-error\t1\t1\t1\t1");
        assertEquals(expected, results);
    }

    @Test
    public void testUnrelatedCompileErrorJavac() throws IOException {
        var tasks = List.of(Task.fromClassName("AddTest", TEST_SRC_DIR).compiler(JAVAC));
        grader.gradeOnly("correct", "unrelated-compile-error");
        grader.run(ECLIPSE_BASE, tasks);
        var results = readAllLines(Path.of("results-AddTest.tsv"));
        var expected = List.of(
                "Name\tcompiled\tcompile errors\ttest compile errors\ttestAdd1\ttestAdd2",
                "correct\t1\t0\t0\t1\t1",
                "unrelated-compile-error\t0\t1\t1\t0\t0");
        assertEquals(expected, results);
    }

    @Test
    public void testCustomDir() throws IOException {
        var tasks = List.of(Task.fromClassName("AddTest", Path.of("tests-custom-dir")));
        grader.gradeOnly("correct", "fails-test", "compile-error");
        grader.run(ECLIPSE_BASE, tasks);
        var results = readAllLines(Path.of("results-AddTest.tsv"));
        var expected = List.of(
                "Name\tcompiled\tcompile errors\ttestAdd",
                "compile-error\t1\t1\t0",
                "correct\t1\t0\t1",
                "fails-test\t1\t0\t0");
        assertEquals(expected, results);
    }

    @Test
    public void testTimeout() throws IOException {
        var tasks = List.of(Task.fromClassName("AddTest", TEST_SRC_DIR).repetitions(3));
        grader.gradeOnly("correct", "infinite-loop"); // contains infinite loop
        grader.run(ECLIPSE_BASE, tasks);
        var results = readAllLines(Path.of("results-AddTest.tsv"));
        var expected = List.of(
                "Name\tcompiled\ttimeout\tincomplete repetitions\ttestAdd1\ttestAdd2",
                "correct\t1\t0\t0\t1\t1",
                "infinite-loop\t1\t1\t1\t0\t0");
        assertEquals(expected, results);
    }

    @Test
    public void testMissingClassUnderTestEclipseCompiler() throws IOException {
        var tasks = List.of(Task.fromClassName("AddTest", TEST_SRC_DIR));
        grader.gradeOnly("correct", "missing-class");
        grader.run(ECLIPSE_BASE, tasks);
        var results = readAllLines(Path.of("results-AddTest.tsv"));
        // TODO: would be nice to have an entry for this
        var expected = List.of(
                "Name\tcompiled\ttest compile errors\ttestAdd1\ttestAdd2",
                "correct\t1\t0\t1\t1",
                "missing-class\t1\t1\t0\t0");
        assertEquals(expected, results);
    }

    @Test
    public void testMissingClassUnderTestJavac() throws IOException {
        var tasks = List.of(Task.fromClassName("AddTest", TEST_SRC_DIR).compiler(JAVAC));
        grader.gradeOnly("correct", "missing-class");
        grader.run(ECLIPSE_BASE, tasks);
        var results = readAllLines(Path.of("results-AddTest.tsv"));
        var expected = List.of(
                "Name\tcompiled\ttest compile errors\ttestAdd1\ttestAdd2",
                "correct\t1\t0\t1\t1",
                "missing-class\t0\t1\t0\t0");
        assertEquals(expected, results);
    }

    @Test
    public void testMissingSrcDir() throws IOException {
        var tasks = List.of(Task.fromClassName("AddTest", TEST_SRC_DIR));
        grader.gradeOnly("correct", "missing-src");
        grader.run(ECLIPSE_BASE, tasks);
        var results = readAllLines(Path.of("results-AddTest.tsv"));
        var expected = List.of(
                "Name\tcompiled\ttest compile errors\ttestAdd1\ttestAdd2",
                "correct\t1\t0\t1\t1",
                "missing-src\t1\t1\t0\t0");
        assertEquals(expected, results);
    }

    @Test
    public void testNondeterminism() throws IOException {
        var tasks = List.of(Task.fromClassName("AddTest", TEST_SRC_DIR)
                .repetitions(50)
                .timeouts(Duration.ofSeconds(5), Duration.ofSeconds(60)));
        grader.gradeOnly("correct", "nondeterministic");
        grader.run(ECLIPSE_BASE, tasks);
        var results = readAllLines(Path.of("results-AddTest.tsv"));
        var expected = List.of(
                "Name\tcompiled\tnondeterministic\ttestAdd1\ttestAdd2",
                "correct\t1\t0\t1\t1",
                "nondeterministic\t1\t1\t0\t0");
        assertEquals(expected, results);
    }

    @Test
    public void testNondeterminismPlusTimeout() throws IOException {
        // ensure that the timeout of one test does not affect detection
        // of nondeterminism in other tests, as was previously the case
        var tasks = List.of(Task.fromClassName("SubtractTest", TEST_SRC_DIR)
                .repetitions(5)
                .permittedCalls(DEFAULT_WHITELIST_DEF + """
                        java.lang.System.setProperty
                        """));
        grader.gradeOnly("correct", "nondeterministic-infinite-loop");
        grader.run(ECLIPSE_BASE, tasks);
        var results = readAllLines(Path.of("results-SubtractTest.tsv"));
        var expected = List.of(
                "Name\tcompiled\tnondeterministic\ttimeout\tincomplete repetitions\ttestSubtract1\ttestSubtract2\ttestSubtract3\ttestSubtract4\ttestSubtract5\ttestSubtract6",
                "correct\t1\t0\t0\t0\t1\t1\t1\t1\t1\t1",
                "nondeterministic-infinite-loop\t1\t1\t1\t1\t0\t0\t0\t0\t0\t0");
        assertEquals(expected, results);
    }

    @Test
    public void testIsolation() throws IOException {
        // ensure that classes are reloaded for each test run, meaning
        // that tests cannot interfere via static fields
        var tasks = List.of(Task.fromClassName("SubtractTest", TEST_SRC_DIR));
        grader.gradeOnly("correct", "static-state");
        grader.run(ECLIPSE_BASE, tasks);
        var results = readAllLines(Path.of("results-SubtractTest.tsv"));
        var expected = List.of(
                "Name\tcompiled\ttestSubtract1\ttestSubtract2\ttestSubtract3\ttestSubtract4\ttestSubtract5\ttestSubtract6",
                "correct\t1\t1\t1\t1\t1\t1\t1",
                "static-state\t1\t1\t1\t1\t1\t1\t1");
        assertEquals(expected, results);
    }

    @Test
    public void testCatchInterruptedException() throws IOException {
        var tasks = List.of(Task.fromClassName("AddTest", TEST_SRC_DIR).repetitions(3));
        grader.gradeOnly("correct", "catch-interrupted-exception"); // contains infinite loop plus catch(InterruptedException)
        grader.run(ECLIPSE_BASE, tasks);
        var results = readAllLines(Path.of("results-AddTest.tsv"));
        var expected = List.of(
                "Name\tcompiled\ttimeout\tincomplete repetitions\ttestAdd1\ttestAdd2",
                "catch-interrupted-exception\t1\t1\t1\t0\t0",
                "correct\t1\t0\t0\t1\t1");
        assertEquals(expected, results);
    }

    @Test
    public void testSystemIn() throws IOException {
        var tasks = List.of(Task.fromClassName("AddTest", TEST_SRC_DIR).repetitions(3));
        grader.gradeOnly("correct", "reads-system-in");
        grader.run(ECLIPSE_BASE, tasks);
        var results = readAllLines(Path.of("results-AddTest.tsv"));
        var expected = List.of(
                "Name\tcompiled\ttestAdd1\ttestAdd2",
                "correct\t1\t1\t1",
                "reads-system-in\t1\t0\t0");
        assertEquals(expected, results);
    }

    @Test
    public void testIllegalOperationIO() throws IOException {
        var tasks = List.of(Task.fromClassName("AddTest", TEST_SRC_DIR));
        grader.gradeOnly("correct", "illegal-io"); // tries to read from the file system
        grader.run(ECLIPSE_BASE, tasks);
        var results = readAllLines(Path.of("results-AddTest.tsv"));
        var expected = List.of(
                "Name\tcompiled\tillegal operation\ttestAdd1\ttestAdd2",
                "correct\t1\t0\t1\t1",
                "illegal-io\t1\t1\t0\t0");
        assertEquals(expected, results);
    }

    @Test
    public void testIllegalOperationReflection() throws IOException {
        var tasks = List.of(Task.fromClassName("AddTest", TEST_SRC_DIR));
        grader.gradeOnly("correct", "illegal-reflect"); // uses getDeclaredMethods()
        grader.run(ECLIPSE_BASE, tasks);
        var results = readAllLines(Path.of("results-AddTest.tsv"));
        var expected = List.of(
                "Name\tcompiled\tillegal operation\ttestAdd1\ttestAdd2",
                "correct\t1\t0\t1\t1",
                "illegal-reflect\t1\t1\t0\t0");
        assertEquals(expected, results);
    }

    @Test
    public void testNoTests() throws IOException {
        var tasks = List.of(Task.fromClassName("NoTests", TEST_SRC_DIR).compiler(ECLIPSE));
        grader.gradeOnly("correct");
        var resultsList = grader.run(ECLIPSE_BASE, tasks);
        assertEquals(1, resultsList.size());
        var results = resultsList.get(0);
        assertEquals(1, results.submissionResults().size());
        var submResults = results.submissionResults().get(0);
        assertEquals(emptyList(), submResults.passedTests());
        assertEquals(emptyList(), submResults.failedTests());
    }

    @Test
    public void testNestedTestClasses() throws IOException {
        var tasks = List.of(Task.fromClassName("NestedTestClass", TEST_SRC_DIR).compiler(ECLIPSE));
        grader.gradeOnly("correct", "compile-error");
        var resultsList = grader.run(ECLIPSE_BASE, tasks);

        assertEquals(1, resultsList.size());
        var results = resultsList.get(0);
        assertEquals(2, results.submissionResults().size());

        var allTests = List.of("MultiplyTest.testMultiply1", "MultiplyTest.testMultiply2"); // note alphabetical order
        assertEquals(allTests, results.get("correct").passedTests());
        assertEquals(emptyList(), results.get("correct").failedTests());
        assertEquals(emptyList(), results.get("compile-error").passedTests());
        assertEquals(allTests, results.get("compile-error").failedTests());
    }

    @Test
    public void testAssumptions() throws IOException {
        var tasks = List.of(Task.fromClassName("TestWithAssumption", TEST_SRC_DIR).compiler(ECLIPSE));
        grader.gradeOnly("correct");
        var resultsList = grader.run(ECLIPSE_BASE, tasks);

        assertEquals(1, resultsList.size());
        var results = resultsList.get(0);
        assertEquals(1, results.submissionResults().size());

        var testResults = results.get("correct").testResults();
        assertTrue(testResults.methodResultFor("testNormal").get().passed());
        assertFalse(testResults.methodResultFor("testFailedAssumption").get().passed());
    }

    @Test
    public void testDisabled() throws IOException {
        var tasks = List.of(Task.fromClassName("DisabledTest", TEST_SRC_DIR).compiler(ECLIPSE));
        grader.gradeOnly("correct");
        var resultsList = grader.run(ECLIPSE_BASE, tasks);

        assertEquals(1, resultsList.size());
        var results = resultsList.get(0);
        assertEquals(1, results.submissionResults().size());

        var testResults = results.get("correct").testResults();
        assertTrue(testResults.methodResultFor("testNormal").get().passed());
        assertTrue(testResults.methodResultFor("testDisabled").get().passed());

        // TODO: better support for disabled tests
    }

    @Test
    public void testDefaultMethodOrder() throws IOException {
        var tasks = List.of(Task.fromClassName("TestWithDisplayNames", TEST_SRC_DIR).compiler(ECLIPSE));
        grader.gradeOnly("correct");
        grader.run(ECLIPSE_BASE, tasks);
        var results = readAllLines(Path.of("results-TestWithDisplayNames.tsv"));
        var expected = List.of(
                "Name\tcompiled\ttest3\ttest2\ttest4\ttest1",
                "correct\t1\t1\t0\t1\t0");
        assertEquals(expected, results);
    }

    @Test
    public void testCustomMethodOrder() throws IOException {
        var tasks = List.of(Task.fromClassName("TestWithMethodOrder", TEST_SRC_DIR).compiler(ECLIPSE));
        grader.gradeOnly("correct");
        grader.run(ECLIPSE_BASE, tasks);
        var results = readAllLines(Path.of("results-TestWithMethodOrder.tsv"));
        var expected = List.of(
                "Name\tcompiled\ttest4\ttest3\ttest2\ttest1",
                "correct\t1\t1\t1\t0\t0");
        assertEquals(expected, results);
    }

    @Test
    public void testScore() throws IOException {
        var tasks = List.of(Task.fromClassName("TestWithScore", TEST_SRC_DIR).compiler(ECLIPSE).repetitions(20));
        grader.gradeOnly("correct", "fails-test");
        var resultsList = grader.run(ECLIPSE_BASE, tasks);

        // submission 0 passes all tests
        var results0 = resultsList.get(0).get("correct").testResults();
        assertTrue(results0.methodResultFor("testWithScore").get().passed());
        var scores = results0.methodResultFor("testWithScore").get().scores();
        assertEquals(20, scores.size());
        assertEquals(Set.of(50.0, 100.0), Set.copyOf(scores));

        assertTrue(results0.methodResultFor("testWithScoreFirst").get().passed());
        scores = results0.methodResultFor("testWithScoreFirst").get().scores();
        assertEquals(20, scores.size());
        assertEquals(Set.of(100.0), Set.copyOf(scores));

        assertTrue(results0.methodResultFor("testWithoutScore").get().passed());
        assertTrue(results0.methodResultFor("testWithoutScore").get().scores().isEmpty());

        assertTrue(results0.methodResultFor("testWithOrWithoutScore").get().passed());
        scores = results0.methodResultFor("testWithOrWithoutScore").get().scores();
        assertTrue(0 < scores.size() && scores.size() < 20);
        assertEquals(Set.of(100.0), Set.copyOf(scores));

        // submission 1 fails all tests
        var results1 = resultsList.get(0).get("fails-test").testResults();
        assertFalse(results1.methodResultFor("testWithScore").get().passed());
        assertTrue(results1.methodResultFor("testWithScore").get().scores().isEmpty());

        assertFalse(results1.methodResultFor("testWithScoreFirst").get().passed());
        scores = results1.methodResultFor("testWithScoreFirst").get().scores();
        assertEquals(20, scores.size());
        assertEquals(Set.of(100.0), Set.copyOf(scores));

        assertFalse(results1.methodResultFor("testWithoutScore").get().passed());
        assertEquals(0, results1.methodResultFor("testWithoutScore").get().scores().size());

        assertFalse(results1.methodResultFor("testWithOrWithoutScore").get().passed());
        assertTrue(results1.methodResultFor("testWithOrWithoutScore").get().scores().isEmpty());
    }

    @Test
    public void testEncoding() throws IOException {
        var tasks = List.of(Task.fromClassName("EncodingTest", TEST_SRC_DIR)
                .compiler(ECLIPSE)
                .permittedCalls(DEFAULT_WHITELIST_DEF + """
                        java.io.InputStreamReader.<init>
                        """));
        var resultsFile = Path.of("results-EncodingTest.tsv");
        grader.gradeOnly("correct", "fails-test");
        grader.run(ECLIPSE_BASE, tasks);
        var results = readAllLines(resultsFile);
        var expected = List.of(
                "Name\tcompiled\ttestEncoding",
                "correct\t1\t1",
                "fails-test\t1\t1");
        assertEquals(expected, results);

        try (var other = new Grader()) {
            other.setTestVmArgs("-Dfile.encoding=ASCII");
            other.gradeOnly("correct", "fails-test");
            other.run(ECLIPSE_BASE, tasks);
            results = readAllLines(resultsFile);
            expected = List.of(
                    "Name\tcompiled\ttestEncoding",
                    "correct\t1\t1",
                    "fails-test\t1\t0");
            assertEquals(expected, results);
        }
    }

    @Test
    public void testClassPathJavac() throws IOException {
        var tasks = List.of(Task.fromClassName("AddTest", TEST_SRC_DIR)
                .compiler(JAVAC)
                .dependencies(Path.of("test-lib/commons-math3-3.6.1.jar"))
                .permittedCalls(DEFAULT_WHITELIST_DEF + """
                        org.apache.commons.math3.util.FastMath.abs
                        """));
        grader.gradeOnly("correct", "fails-test", "external-lib"); // 15 uses external lib
        grader.run(ECLIPSE_BASE, tasks);
        var results = readAllLines(Path.of("results-AddTest.tsv"));
        var expected = List.of(
                "Name\tcompiled\ttestAdd1\ttestAdd2",
                "correct\t1\t1\t1",
                "external-lib\t1\t1\t1",
                "fails-test\t1\t1\t0");
        assertEquals(expected, results);
    }

    @Test
    public void testClassPathEclipse() throws IOException {
        var tasks = List.of(Task.fromClassName("AddTest", TEST_SRC_DIR)
                .compiler(ECLIPSE)
                .dependencies(Path.of("test-lib/commons-math3-3.6.1.jar"))
                .permittedCalls(DEFAULT_WHITELIST_DEF + """
                        org.apache.commons.math3.util.FastMath.abs
                        """));
        grader.gradeOnly("correct", "fails-test", "external-lib"); // 15 uses external lib
        grader.run(ECLIPSE_BASE, tasks);
        var results = readAllLines(Path.of("results-AddTest.tsv"));
        var expected = List.of(
                "Name\tcompiled\ttestAdd1\ttestAdd2",
                "correct\t1\t1\t1",
                "external-lib\t1\t1\t1",
                "fails-test\t1\t1\t0");
        assertEquals(expected, results);
    }

    @AfterAll
    public static void tearDown() throws IOException {
        grader.close();
        try (var allFiles = list(Path.of("."))) {
            allFiles.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString()
                            .matches("results-.*\\.tsv"))
                    .forEach(p -> p.toFile().delete());
        }
    }
}
