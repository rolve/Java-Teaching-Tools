package ch.trick17.jtt.sandbox;

import ch.trick17.jtt.sandbox.SandboxResult.Kind;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static ch.trick17.jtt.sandbox.InputMode.CLOSED;
import static ch.trick17.jtt.sandbox.InputMode.EMPTY;
import static ch.trick17.jtt.sandbox.OutputMode.*;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class SandboxTest {

    // Many features are tested indirectly in GraderTest, not here

    private static ByteArrayInputStream inSupplier;
    private static ByteArrayOutputStream outRecorder;
    private static ByteArrayOutputStream errRecorder;

    @BeforeAll
    public static void install() {
        inSupplier = new ByteArrayInputStream("Hello, World!".getBytes());
        outRecorder = new ByteArrayOutputStream();
        errRecorder = new ByteArrayOutputStream();
        System.setIn(inSupplier);
        System.setOut(new PrintStream(outRecorder));
        System.setErr(new PrintStream(errRecorder));
    }

    @BeforeEach
    public void reset() {
        inSupplier.reset();
        outRecorder.reset();
        errRecorder.reset();
    }

    @Test
    public void testInputModeNormal() {
        var sandbox = new Sandbox().stdInMode(InputMode.NORMAL);
        var result = sandbox.run(code(), emptyList(), InputTestCode.class, "run",
                emptyList(), emptyList(), String.class);
        assertEquals(Kind.NORMAL, result.kind());
        assertEquals("Hello", result.value());
    }

    @Test
    public void testInputModeEmpty() {
        var sandbox = new Sandbox().stdInMode(EMPTY);
        var result = sandbox.run(code(), emptyList(), InputTestCode.class, "run",
                emptyList(), emptyList(), String.class);
        assertEquals(Kind.NORMAL, result.kind());
        assertEquals("", result.value());
    }

    @Test
    public void testInputModeClosed() {
        var sandbox = new Sandbox().stdInMode(CLOSED);
        var result = sandbox.run(code(), emptyList(), InputTestCode.class, "run",
                emptyList(), emptyList(), String.class);
        assertEquals(Kind.EXCEPTION, result.kind());
        assertEquals(IOException.class, result.exception().getClass());
    }

    @Test
    public void testOutputModeNormal() {
        var sandbox = new Sandbox()
                .stdOutMode(NORMAL)
                .stdErrMode(NORMAL);
        var result = sandbox.run(code(), emptyList(), OutputTestCode.class, "run",
                emptyList(), emptyList(), Void.class);

        assertNull(result.stdOut());
        assertNull(result.stdErr());
        assertEquals("This goes out", outRecorder.toString());
        assertEquals("This goes err", errRecorder.toString());
    }

    @Test
    public void testOutputModeDiscard() {
        var sandbox = new Sandbox()
                .stdOutMode(DISCARD)
                .stdErrMode(DISCARD);
        var result = sandbox.run(code(), emptyList(), OutputTestCode.class, "run",
                emptyList(), emptyList(), Void.class);

        assertNull(result.stdOut());
        assertNull(result.stdErr());
        assertEquals("", outRecorder.toString());
        assertEquals("", errRecorder.toString());
    }

    @Test
    public void testOutputModeRecord() {
        var sandbox = new Sandbox()
                .stdOutMode(RECORD)
                .stdErrMode(RECORD);
        var result = sandbox.run(code(), emptyList(), OutputTestCode.class, "run",
                emptyList(), emptyList(), Void.class);

        assertEquals("This goes out", result.stdOut());
        assertEquals("This goes err", result.stdErr());
        assertEquals("", outRecorder.toString());
        assertEquals("", errRecorder.toString());
    }

    @Test
    public void testOutputModeRecordTimeout() {
        var sandbox = new Sandbox()
                .timeout(Duration.ofSeconds(1))
                .stdOutMode(RECORD)
                .stdErrMode(RECORD);
        var result = sandbox.run(code(), emptyList(), OutputTestCode.class, "run",
                emptyList(), emptyList(), Void.class);

        assertEquals("This goes out", result.stdOut());
        assertEquals("This goes err", result.stdErr());
        assertEquals("", outRecorder.toString());
        assertEquals("", errRecorder.toString());
    }

    @Test
    public void testOutputModeRecordForward() {
        var sandbox = new Sandbox()
                .stdOutMode(RECORD_FORWARD)
                .stdErrMode(RECORD_FORWARD);
        var result = sandbox.run(code(), emptyList(), OutputTestCode.class, "run",
                emptyList(), emptyList(), Void.class);

        assertEquals("This goes out", result.stdOut());
        assertEquals("This goes err", result.stdErr());
        assertEquals("This goes out", outRecorder.toString());
        assertEquals("This goes err", errRecorder.toString());
    }

    private List<Path> code() {
        var url = SandboxTest.class.getProtectionDomain().getCodeSource().getLocation();
        try {
            return List.of(Path.of(url.toURI()));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static class InputTestCode {
        public static String run() throws IOException {
            return new String(System.in.readNBytes(5));
        }
    }

    public static class OutputTestCode {
        public static void run() {
            System.out.print("This goes out");
            System.err.print("This goes err");
        }
    }
}
