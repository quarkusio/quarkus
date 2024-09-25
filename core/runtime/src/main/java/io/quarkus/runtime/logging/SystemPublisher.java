package io.quarkus.runtime.logging;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;

import org.jboss.logging.Logger;

/**
 * System.(out/err) -> Logging
 */
public class SystemPublisher {
    private static final Logger LOG = Logger.getLogger("System");
    private final PipedOutputStream outPos;
    private final PipedInputStream outPis;
    private final PrintStream out;

    private final PipedOutputStream errPos;
    private final PipedInputStream errPis;
    private final PrintStream err;

    private SystemPublisher() throws IOException {
        outPos = new PipedOutputStream();
        outPis = new PipedInputStream(outPos);
        out = new PrintStream(outPos, true);

        errPos = new PipedOutputStream();
        errPis = new PipedInputStream(errPos);
        err = new PrintStream(errPos, true);

        this.startPublishing();
    }

    private void startPublishing() {
        System.setOut(out);
        System.setErr(err);

        capture(outPis, Logger.Level.INFO);
        capture(errPis, Logger.Level.ERROR);
    }

    private void capture(PipedInputStream pis, Logger.Level level) {
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(pis))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    LOG.log(level, line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                closeStreams();
            }
        }).start();
    }

    private void closeStreams() {
        try {
            outPis.close();
            outPos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void init() {
        try {
            new SystemPublisher();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}
