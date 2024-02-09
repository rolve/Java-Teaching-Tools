package ch.trick17.jtt.grader;

import ch.trick17.jtt.memcompile.Compiler;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static ch.trick17.jtt.grader.GraderTest.SUBM_ROOT;
import static ch.trick17.jtt.grader.GraderTest.TEST_SRC_DIR;
import static ch.trick17.jtt.memcompile.Compiler.ECLIPSE;
import static ch.trick17.jtt.memcompile.Compiler.JAVAC;
import static ch.trick17.jtt.sandbox.Whitelist.DEFAULT_WHITELIST_DEF;
import static java.nio.file.Files.list;
import static java.nio.file.Files.readString;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class BatchGraderTest {

    static final List<Submission> WITH_ECLIPSE_STRUCTURE;
    static final List<Submission> WITH_MVN_STRUCTURE;

    static {
        try {
            WITH_ECLIPSE_STRUCTURE = Submission.loadAllFrom(
                    SUBM_ROOT.resolve("eclipse-structure"), Path.of("src"));
            WITH_MVN_STRUCTURE = Submission.loadAllFrom(
                    SUBM_ROOT.resolve("maven-structure"), Path.of("src/main/java"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static final String EXPECTED_ADD_SIMPLE_EC = withTabs("""
            Name           compiled  compile errors  add1  add2
            compile-error  1         1               0     0
            correct        1         0               1     1
            fails-test     1         0               1     0
            """);

    static final String EXPECTED_ADD_SIMPLE_JC = withTabs("""
            Name           compiled  compile errors  test compile errors  add1  add2
            compile-error  0         1               1                    0     0
            correct        1         0               0                    1     1
            fails-test     1         0               0                    1     0
            """);

    static BatchGrader grader = new BatchGrader(null, Path.of("."));

    @Test
    void eclipseStructureEclipseCompiler() throws IOException {
        var task = Task.fromClassName("AddTest", TEST_SRC_DIR).compiler(ECLIPSE);
        var submissions = WITH_ECLIPSE_STRUCTURE.stream()
                .filter(s -> List.of("correct", "fails-test", "compile-error").contains(s.name()))
                .toList();
        grader.grade(task, submissions);
        var results = readString(Path.of("results-AddTest.tsv"));
        assertEquals(EXPECTED_ADD_SIMPLE_EC, results);
    }

    @Test
    void eclipseStructureJavac() throws IOException {
        var task = Task.fromClassName("AddTest", TEST_SRC_DIR).compiler(JAVAC);
        var submissions = WITH_ECLIPSE_STRUCTURE.stream()
                .filter(s -> List.of("correct", "fails-test", "compile-error").contains(s.name()))
                .toList();
        grader.grade(task, submissions);
        var results = readString(Path.of("results-AddTest.tsv"));
        assertEquals(EXPECTED_ADD_SIMPLE_JC, results);
    }

    @Test
    void mavenStructureEclipseCompiler() throws IOException {
        var task = Task.fromClassName("AddTest", TEST_SRC_DIR).compiler(ECLIPSE);
        grader.grade(task, WITH_MVN_STRUCTURE);
        var results = readString(Path.of("results-AddTest.tsv"));
        assertEquals(EXPECTED_ADD_SIMPLE_EC, results);
    }

    @Test
    void mavenStructureJavac() throws IOException {
        var task = Task.fromClassName("AddTest", TEST_SRC_DIR).compiler(JAVAC);
        grader.grade(task, WITH_MVN_STRUCTURE);
        var results = readString(Path.of("results-AddTest.tsv"));
        assertEquals(EXPECTED_ADD_SIMPLE_JC, results);
    }

    @Test
    void packageEclipseCompiler() throws IOException {
        var task = Task.fromClassName("multiply.MultiplyTest", TEST_SRC_DIR).compiler(ECLIPSE);
        var submissions = WITH_ECLIPSE_STRUCTURE.stream()
                .filter(s -> List.of("correct", "fails-test", "compile-error").contains(s.name()))
                .toList();
        grader.grade(task, submissions);
        var results = readString(Path.of("results-MultiplyTest.tsv"));
        var expected = withTabs("""
                Name           compiled  compile errors  multiply1  multiply2
                compile-error  1         1               0          0
                correct        1         0               1          1
                fails-test     1         0               1          0
                """);
        assertEquals(expected, results);

        task = Task.fromClassName("com.example.foo.FooTest", TEST_SRC_DIR).compiler(ECLIPSE);
        submissions = WITH_ECLIPSE_STRUCTURE.stream()
                .filter(s -> s.name().equals("correct"))
                .toList();
        grader.grade(task, submissions);
        results = readString(Path.of("results-FooTest.tsv"));
        expected = withTabs("""
                Name     compiled  greeting  greetingImpl
                correct  1         1         1
                """);
        assertEquals(expected, results);
    }

    @Test
    void packageJavac() throws IOException {
        var task = Task.fromClassName("multiply.MultiplyTest", TEST_SRC_DIR).compiler(JAVAC);
        var submissions = WITH_ECLIPSE_STRUCTURE.stream()
                .filter(s -> List.of("correct", "fails-test", "compile-error").contains(s.name()))
                .toList();
        grader.grade(task, submissions);
        var results = readString(Path.of("results-MultiplyTest.tsv"));
        var expected = withTabs("""
                Name           compiled  compile errors  test compile errors  multiply1  multiply2
                compile-error  0         1               1                    0          0
                correct        1         0               0                    1          1
                fails-test     1         0               0                    1          0
                """);
        assertEquals(expected, results);

        task = Task.fromClassName("com.example.foo.FooTest", TEST_SRC_DIR).compiler(JAVAC);
        submissions = WITH_ECLIPSE_STRUCTURE.stream()
                .filter(s -> s.name().equals("correct"))
                .toList();
        grader.grade(task, submissions);
        results = readString(Path.of("results-FooTest.tsv"));
        expected = withTabs("""
                Name     compiled  greeting  greetingImpl
                correct  1         1         1
                """);
        assertEquals(expected, results);
    }

    @Test
    void unrelatedCompileErrorEclipseCompiler() throws IOException {
        var task = Task.fromClassName("AddTest", TEST_SRC_DIR).compiler(ECLIPSE);
        var submissions = WITH_ECLIPSE_STRUCTURE.stream()
                .filter(s -> List.of("correct", "unrelated-compile-error").contains(s.name()))
                .toList();
        grader.grade(task, submissions);
        var results = readString(Path.of("results-AddTest.tsv"));
        var expected = withTabs("""
                Name                     compiled  compile errors  add1  add2
                correct                  1         0               1     1
                unrelated-compile-error  1         1               1     1
                """);
        assertEquals(expected, results);
    }

    @Test
    void unrelatedCompileErrorJavac() throws IOException {
        var task = Task.fromClassName("AddTest", TEST_SRC_DIR).compiler(JAVAC);
        var submissions = WITH_ECLIPSE_STRUCTURE.stream()
                .filter(s -> List.of("correct", "unrelated-compile-error").contains(s.name()))
                .toList();
        grader.grade(task, submissions);
        var results = readString(Path.of("results-AddTest.tsv"));
        var expected = withTabs("""
                Name                     compiled  compile errors  test compile errors  add1  add2
                correct                  1         0               0                    1     1
                unrelated-compile-error  0         1               1                    0     0
                """);
        assertEquals(expected, results);
    }

    @Test
    void customDir() throws IOException {
        var task = Task.fromClassName("AddTest", Path.of("tests-custom-dir"));
        var submissions = WITH_ECLIPSE_STRUCTURE.stream()
                .filter(s -> List.of("correct", "fails-test", "compile-error").contains(s.name()))
                .toList();
        grader.grade(task, submissions);
        var results = readString(Path.of("results-AddTest.tsv"));
        var expected = withTabs("""
                Name           compiled  compile errors  add
                compile-error  1         1               0
                correct        1         0               1
                fails-test     1         0               0
                """);
        assertEquals(expected, results);
    }

    @Test
    void timeout() throws IOException {
        var task = Task.fromClassName("AddTest", TEST_SRC_DIR)
                .repetitions(5)
                .timeouts(Duration.ofSeconds(1), Duration.ofSeconds(3));
        var submissions = WITH_ECLIPSE_STRUCTURE.stream()
                .filter(s -> List.of("correct", "infinite-loop").contains(s.name()))
                .toList(); // contains infinite loop
        grader.grade(task, submissions);
        var results = readString(Path.of("results-AddTest.tsv"));
        var expected = withTabs("""
                Name           compiled  timeout  incomplete repetitions  add1  add2
                correct        1         0        0                       1     1
                infinite-loop  1         1        1                       0     0
                """);
        assertEquals(expected, results);
    }

    @Test
    void missingClassUnderTestEclipseCompiler() throws IOException {
        var task = Task.fromClassName("AddTest", TEST_SRC_DIR);
        var submissions = WITH_ECLIPSE_STRUCTURE.stream()
                .filter(s -> List.of("correct", "missing-class").contains(s.name()))
                .toList();
        grader.grade(task, submissions);
        var results = readString(Path.of("results-AddTest.tsv"));
        // TODO: would be nice to have an entry for this
        var expected = withTabs("""
                Name           compiled  test compile errors  add1  add2
                correct        1         0                    1     1
                missing-class  1         1                    0     0
                """);
        assertEquals(expected, results);
    }

    @Test
    void missingClassUnderTestJavac() throws IOException {
        var task = Task.fromClassName("AddTest", TEST_SRC_DIR).compiler(JAVAC);
        var submissions = WITH_ECLIPSE_STRUCTURE.stream()
                .filter(s -> List.of("correct", "missing-class").contains(s.name()))
                .toList();
        grader.grade(task, submissions);
        var results = readString(Path.of("results-AddTest.tsv"));
        var expected = withTabs("""
                Name           compiled  test compile errors  add1  add2
                correct        1         0                    1     1
                missing-class  0         1                    0     0
                """);
        assertEquals(expected, results);
    }

    @Test
    void missingSrcDir() throws IOException {
        var task = Task.fromClassName("AddTest", TEST_SRC_DIR);
        var submissions = WITH_ECLIPSE_STRUCTURE.stream()
                .filter(s -> List.of("correct", "missing-src").contains(s.name()))
                .toList();
        grader.grade(task, submissions);
        var results = readString(Path.of("results-AddTest.tsv"));
        var expected = withTabs("""
                Name         compiled  test compile errors  add1  add2
                correct      1         0                    1     1
                missing-src  1         1                    0     0
                """);
        assertEquals(expected, results);
    }

    @Test
    void nondeterminism() throws IOException {
        var task = Task.fromClassName("AddTest", TEST_SRC_DIR)
                .repetitions(20)
                .timeouts(Duration.ofSeconds(5), Duration.ofSeconds(30));
        var submissions = WITH_ECLIPSE_STRUCTURE.stream()
                .filter(s -> List.of("correct", "nondeterministic").contains(s.name()))
                .toList();
        grader.grade(task, submissions);
        var results = readString(Path.of("results-AddTest.tsv"));
        var expected = withTabs("""
                Name              compiled  nondeterministic  add1  add2
                correct           1         0                 1     1
                nondeterministic  1         1                 0     0
                """);
        assertEquals(expected, results);
    }

    @Test
    void nondeterminismPlusTimeout() throws IOException {
        // ensure that the timeout of one test does not affect detection
        // of nondeterminism in other s, as was previously the case
        var task = Task.fromClassName("SubtractTest", TEST_SRC_DIR)
                .repetitions(5)
                .timeouts(Duration.ofSeconds(1), Duration.ofSeconds(3))
                .permittedCalls(DEFAULT_WHITELIST_DEF + """
                        java.lang.System.setProperty
                        """);
        var submissions = WITH_ECLIPSE_STRUCTURE.stream()
                .filter(s -> List.of("correct", "nondeterministic-infinite-loop").contains(s.name()))
                .toList();
        grader.grade(task, submissions);
        var results = readString(Path.of("results-SubtractTest.tsv"));
        var expected = withTabs("""
                Name                            compiled  nondeterministic  timeout  incomplete repetitions  subtract1  subtract2  subtract3  subtract4  subtract5  subtract6
                correct                         1         0                 0        0                       1          1          1          1          1          1
                nondeterministic-infinite-loop  1         1                 1        1                       0          0          0          0          0          0
                """);
        assertEquals(expected, results);
    }

    @Test
    void isolation() throws IOException {
        // ensure that classes are reloaded for each test run, meaning
        // that s cannot interfere via static fields
        var task = Task.fromClassName("SubtractTest", TEST_SRC_DIR);
        var submissions = WITH_ECLIPSE_STRUCTURE.stream()
                .filter(s -> List.of("correct", "static-state").contains(s.name()))
                .toList();
        grader.grade(task, submissions);
        var results = readString(Path.of("results-SubtractTest.tsv"));
        var expected = withTabs("""
                Name          compiled  subtract1  subtract2  subtract3  subtract4  subtract5  subtract6
                correct       1         1          1          1          1          1          1
                static-state  1         1          1          1          1          1          1
                """);
        assertEquals(expected, results);
    }

    @Test
    void catchInterruptedException() throws IOException {
        var task = Task.fromClassName("AddTest", TEST_SRC_DIR)
                .repetitions(5)
                .timeouts(Duration.ofSeconds(1), Duration.ofSeconds(3));
        var submissions = WITH_ECLIPSE_STRUCTURE.stream()
                .filter(s -> List.of("correct", "catch-interrupted-exception").contains(s.name()))
                .toList(); // contains infinite loop plus catch(InterruptedException)
        grader.grade(task, submissions);
        var results = readString(Path.of("results-AddTest.tsv"));
        var expected = withTabs("""
                Name                         compiled  timeout  incomplete repetitions  add1  add2
                catch-interrupted-exception  1         1        1                       0     0
                correct                      1         0        0                       1     1
                """);
        assertEquals(expected, results);
    }

    @Test
    void systemIn() throws IOException {
        var task = Task.fromClassName("AddTest", TEST_SRC_DIR).repetitions(3);
        var submissions = WITH_ECLIPSE_STRUCTURE.stream()
                .filter(s -> List.of("correct", "reads-system-in").contains(s.name()))
                .toList();
        grader.grade(task, submissions);
        var results = readString(Path.of("results-AddTest.tsv"));
        var expected = withTabs("""
                Name             compiled  add1  add2
                correct          1         1     1
                reads-system-in  1         0     0
                """);
        assertEquals(expected, results);
    }

    @Test
    void illegalOperationIO() throws IOException {
        var task = Task.fromClassName("AddTest", TEST_SRC_DIR);
        var submissions = WITH_ECLIPSE_STRUCTURE.stream()
                .filter(s -> List.of("correct", "illegal-io").contains(s.name()))
                .toList(); // tries to read from the file system
        grader.grade(task, submissions);
        var results = readString(Path.of("results-AddTest.tsv"));
        var expected = withTabs("""
                Name         compiled  illegal operation  add1  add2
                correct      1         0                  1     1
                illegal-io   1         1                  0     0
                """);
        assertEquals(expected, results);
    }

    @Test
    void illegalOperationReflection() throws IOException {
        var task = Task.fromClassName("AddTest", TEST_SRC_DIR);
        var submissions = WITH_ECLIPSE_STRUCTURE.stream()
                .filter(s -> List.of("correct", "illegal-reflect").contains(s.name()))
                .toList(); // uses getDeclaredMethods()
        grader.grade(task, submissions);
        var results = readString(Path.of("results-AddTest.tsv"));
        var expected = withTabs("""
                Name             compiled  illegal operation  add1  add2
                correct          1         0                  1     1
                illegal-reflect  1         1                  0     0
                """);
        assertEquals(expected, results);
    }

    @Test
    void defaultMethodOrder() throws IOException {
        var task = Task.fromClassName("TestWithDisplayNames", TEST_SRC_DIR).compiler(ECLIPSE);
        var submissions = WITH_ECLIPSE_STRUCTURE.stream()
                .filter(s -> s.name().equals("correct"))
                .toList();
        grader.grade(task, submissions);
        var results = readString(Path.of("results-TestWithDisplayNames.tsv"));
        var expected = withTabs("""
                Name     compiled  test3  test2  test4  test1
                correct  1         1      0      1      0
                """);
        assertEquals(expected, results);
    }

    @Test
    void customMethodOrder() throws IOException {
        var task = Task.fromClassName("TestWithMethodOrder", TEST_SRC_DIR).compiler(ECLIPSE);
        var submissions = WITH_ECLIPSE_STRUCTURE.stream()
                .filter(s -> s.name().equals("correct"))
                .toList();
        grader.grade(task, submissions);
        var results = readString(Path.of("results-TestWithMethodOrder.tsv"));
        var expected = withTabs("""
                Name     compiled  test4  test3  test2  test1
                correct  1         1      1      0      0
                """);
        assertEquals(expected, results);
    }

    @Test
    void encoding() throws IOException {
        var task = Task.fromClassName("EncodingTest", TEST_SRC_DIR)
                .compiler(ECLIPSE)
                .permittedCalls(DEFAULT_WHITELIST_DEF + """
                        java.io.InputStreamReader.<init>
                        """);
        var resultsFile = Path.of("results-EncodingTest.tsv");
        var submissions = WITH_ECLIPSE_STRUCTURE.stream()
                .filter(s -> List.of("correct", "fails-test").contains(s.name()))
                .toList();
        grader.grade(task, submissions);
        var results = readString(resultsFile);
        var expected = withTabs("""
                Name        compiled  encoding
                correct     1         1
                fails-test  1         1
                """);
        assertEquals(expected, results);

        task = Task.fromClassName("EncodingTest", TEST_SRC_DIR)
                .compiler(ECLIPSE)
                .permittedCalls(DEFAULT_WHITELIST_DEF + """
                        java.io.InputStreamReader.<init>
                        """)
                .testVmArgs("-Dfile.encoding=ASCII");
        grader.grade(task, submissions);
        results = readString(resultsFile);
        expected = withTabs("""
                Name        compiled  encoding
                correct     1         1
                fails-test  1         0
                """);
        assertEquals(expected, results);
    }

    @ParameterizedTest
    @EnumSource(Compiler.class)
    void classPath(Compiler compiler) throws IOException {
        var task = Task.fromClassName("AddTest", TEST_SRC_DIR)
                .compiler(compiler)
                .dependencies(Path.of("test-lib/commons-math3-3.6.1.jar"))
                .permittedCalls(DEFAULT_WHITELIST_DEF + """
                        org.apache.commons.math3.util.FastMath.abs
                        """);
        var submissions = WITH_ECLIPSE_STRUCTURE.stream()
                .filter(s -> List.of("correct", "fails-test", "external-lib").contains(s.name()))
                .toList(); // 15 uses external lib
        grader.grade(task, submissions);
        var results = readString(Path.of("results-AddTest.tsv"));
        var expected = withTabs("""
                Name           compiled  add1  add2
                correct        1         1     1
                external-lib   1         1     1
                fails-test     1         1     0
                """);
        assertEquals(expected, results);
    }

    @Test
    void modificationToGivenClass() throws IOException {
        var task = Task.fromClassName("students.StudentClientTest",
                TEST_SRC_DIR, "students/Student.java");
        var submissions = WITH_ECLIPSE_STRUCTURE.stream()
                .filter(s -> List.of("correct", "modifies-given-class").contains(s.name()))
                .toList();
        grader.grade(task, submissions);
        var results = readString(Path.of("results-StudentClientTest.tsv"));
        var expected = withTabs("""
                Name                  compiled  compile errors  fullName
                correct               1         0               1
                modifies-given-class  1         1               0
                """);
        assertEquals(expected, results);
    }

    @AfterAll
    static void tearDown() throws IOException {
        grader.close();
        try (var allFiles = list(Path.of("."))) {
            allFiles.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString()
                            .matches("results-.*\\.tsv"))
                    .forEach(p -> p.toFile().delete());
        }
    }

    static String withTabs(String text) {
        return text.replaceAll("\s\s+", "\t");
    }
}
