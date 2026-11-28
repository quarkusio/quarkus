package io.quarkus.analytics.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(30)
class InterruptibleLineReaderTest {

    @Test
    void readsOneLineAndLeavesTheRestOfTheInput() throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream("y\nh".getBytes(StandardCharsets.UTF_8));

        assertEquals("y", InterruptibleLineReader.readLine(in));
        assertEquals('h', in.read(), "Input after the line must be left for the next reader");
    }

    @Test
    void stripsWindowsLineTerminator() throws IOException {
        assertEquals("n", InterruptibleLineReader.readLine(new ByteArrayInputStream("n\r\n".getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    void endOfStreamWithoutLineTerminatorWaitsUntilInterrupted() throws Exception {
        ByteArrayInputStream in = new ByteArrayInputStream("yes".getBytes(StandardCharsets.UTF_8));

        assertNull(readInBackgroundThenInterrupt(in));
    }

    @Test
    void stopsReadingWhenInterrupted() throws Exception {
        try (PipedOutputStream out = new PipedOutputStream();
                PipedInputStream in = new PipedInputStream(out)) {
            assertNull(readInBackgroundThenInterrupt(in));

            // input typed after the prompt gave up must reach whoever reads the console next
            out.write('h');
            out.flush();
            assertEquals('h', in.read());
        }
    }

    @Test
    void interruptiblyReadsOneLineAndLeavesTheRestOfTheInput() throws IOException {
        ForwardedInputStream in = new ForwardedInputStream();
        in.type("n\r\nh");

        assertEquals("n", InterruptibleLineReader.readLineInterruptibly(in));
        assertEquals('h', in.read(), "Input after the line must be left for the next reader");
    }

    @Test
    void interruptiblyStopsReadingWhenInterrupted() throws Exception {
        ForwardedInputStream in = new ForwardedInputStream();
        AtomicReference<String> result = new AtomicReference<>("not set");
        Thread reader = new Thread(() -> {
            try {
                result.set(InterruptibleLineReader.readLineInterruptibly(in));
            } catch (IOException e) {
                result.set("failed: " + e);
            }
        });
        reader.start();
        Thread.sleep(200);
        reader.interrupt();
        reader.join(TimeUnit.SECONDS.toMillis(5));
        assertFalse(reader.isAlive(), "The reader must stop once interrupted");
        assertNull(result.get());

        // input typed after the prompt gave up must reach whoever reads the console next
        in.type("h\n");
        assertEquals('h', in.read());
    }

    private static String readInBackgroundThenInterrupt(InputStream in) throws InterruptedException {
        AtomicReference<String> result = new AtomicReference<>("not set");
        Thread reader = new Thread(() -> {
            try {
                result.set(InterruptibleLineReader.readLine(in));
            } catch (IOException e) {
                result.set("failed: " + e);
            }
        });
        reader.start();
        // give the reader time to consume whatever is available before interrupting it
        Thread.sleep(200);
        reader.interrupt();
        reader.join(TimeUnit.SECONDS.toMillis(5));
        assertFalse(reader.isAlive(), "The reader must stop once interrupted");
        return result.get();
    }
}
