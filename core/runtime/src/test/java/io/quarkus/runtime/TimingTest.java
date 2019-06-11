package io.quarkus.runtime;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;
import java.util.logging.XMLFormatter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TimingTest {

    private static Logger log = Logger.getLogger("io.quarkus");
    private static OutputStream logCapturingStream;
    private static StreamHandler customLogHandler;

    @Before
    public void setup() {
        logCapturingStream = new ByteArrayOutputStream();
        customLogHandler = new StreamHandler(logCapturingStream, new XMLFormatter());
        log.addHandler(customLogHandler);
    }

    private String getTestCapturedLog() {
        customLogHandler.flush();
        return logCapturingStream.toString();
    }

    @Test
    public void artifactAndVersionExists() {
        Timing.printStartupTime("0.16", "cdi", "myArtifactId", "1.0.0");
        String capturedLog = getTestCapturedLog();

        Assert.assertTrue(capturedLog.contains("%s %s (Running on Quarkus %s) started in %ss. %s"));
        Assert.assertTrue(capturedLog.contains("<param>0.16</param>"));
        Assert.assertTrue(capturedLog.contains("<param>myArtifactId</param>"));
        Assert.assertTrue(capturedLog.contains("<param>cdi</param>"));

    }

    @Test
    public void missingArtifactAndVersionExists() {
        Timing.printStartupTime("0.16", "cdi", "<<unset>>", "<<unset>>");
        String capturedLog = getTestCapturedLog();

        Assert.assertTrue(capturedLog.contains("Quarkus %s started in %ss. %s"));
        Assert.assertTrue(capturedLog.contains("<param>0.16</param>"));
        Assert.assertTrue(!capturedLog.contains("<param><<unset>></param>"));
        Assert.assertTrue(capturedLog.contains("<param>cdi</param>"));

    }
}
