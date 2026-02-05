package io.quarkus.jlink.deployment;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Tests for {@link LogWriter}.
 */
class LogWriterTest {

    private final Logger logger = mock(Logger.class);

    @Test
    void writeStringLogsOnNewline() {
        LogWriter lw = new LogWriter(logger, Logger.Level.INFO);
        lw.write("hello\n");
        verify(logger).log(Logger.Level.INFO, "hello");
    }

    @Test
    void writeStringLogsOnCarriageReturn() {
        LogWriter lw = new LogWriter(logger, Logger.Level.INFO);
        lw.write("hello\r");
        verify(logger).log(Logger.Level.INFO, "hello");
    }

    @Test
    void writeStringCrLfLogsOnce() {
        LogWriter lw = new LogWriter(logger, Logger.Level.INFO);
        lw.write("hello\r\n");
        verify(logger, Mockito.times(1)).log(Logger.Level.INFO, "hello");
    }

    @Test
    void writeIntLogsOnNewline() {
        LogWriter lw = new LogWriter(logger, Logger.Level.INFO);
        lw.write('A');
        lw.write('B');
        verifyNoInteractions(logger);
        lw.write('\n');
        verify(logger).log(Logger.Level.INFO, "AB");
    }

    @Test
    void writeCharArrayLogsOnNewline() {
        LogWriter lw = new LogWriter(logger, Logger.Level.INFO);
        char[] chars = "test\n".toCharArray();
        lw.write(chars, 0, chars.length);
        verify(logger).log(Logger.Level.INFO, "test");
    }

    @Test
    void flushLogsPartialLine() {
        LogWriter lw = new LogWriter(logger, Logger.Level.INFO);
        lw.write("partial");
        verifyNoInteractions(logger);
        lw.flush();
        verify(logger).log(Logger.Level.INFO, "partial");
    }

    @Test
    void emptyLineIsNotLogged() {
        LogWriter lw = new LogWriter(logger, Logger.Level.INFO);
        lw.write("\n");
        verify(logger, never()).log(Mockito.any(Logger.Level.class), Mockito.anyString());
    }

    @Test
    void flushAt1024Chars() {
        LogWriter lw = new LogWriter(logger, Logger.Level.INFO);
        // write exactly 1024 chars without a newline
        String line = "x".repeat(1024);
        lw.write(line);
        verify(logger).log(Logger.Level.INFO, line);
    }

    @Test
    void appendCharSequence() {
        LogWriter lw = new LogWriter(logger, Logger.Level.INFO);
        lw.append("hello world", 0, 5);
        lw.write('\n');
        verify(logger).log(Logger.Level.INFO, "hello");
    }

    @Test
    void closeIsNoOp() {
        LogWriter lw = new LogWriter(logger, Logger.Level.INFO);
        lw.write("unflushed");
        lw.close();
        // close does not flush, so nothing should be logged
        verifyNoInteractions(logger);
    }

    @Test
    void multipleLinesInOneWrite() {
        LogWriter lw = new LogWriter(logger, Logger.Level.INFO);
        lw.write("a\nb\nc\n");
        verify(logger).log(Logger.Level.INFO, "a");
        verify(logger).log(Logger.Level.INFO, "b");
        verify(logger).log(Logger.Level.INFO, "c");
    }

    @Test
    void usesCorrectLogLevel() {
        LogWriter lw = new LogWriter(logger, Logger.Level.WARN);
        lw.write("warning\n");
        verify(logger).log(Logger.Level.WARN, "warning");
        verify(logger, never()).log(Mockito.eq(Logger.Level.INFO), Mockito.anyString());
    }
}
