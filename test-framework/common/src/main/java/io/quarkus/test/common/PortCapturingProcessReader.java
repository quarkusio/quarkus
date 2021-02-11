package io.quarkus.test.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.wildfly.common.lock.Locks;

final class PortCapturingProcessReader extends ProcessReader {
    private Integer port;

    private boolean portDetermined = false;
    private StringBuilder sb = new StringBuilder();
    private final Lock lock = Locks.reentrantLock();
    private final Condition portDeterminedCondition = lock.newCondition();
    private final Pattern portRegex = Pattern.compile("Listening on:\\s+https?://.*:(\\d+)");

    PortCapturingProcessReader(InputStream inputStream) {
        super(inputStream);
    }

    @Override
    protected void handleStart() {
        lock.lock();
    }

    @Override
    protected void handleString(String str) {
        if (portDetermined) { // we are done with determining the port
            return;
        }
        sb.append(str);
        String currentOutput = sb.toString();
        Matcher regexMatcher = portRegex.matcher(currentOutput);
        if (!regexMatcher.find()) { // haven't read enough data yet
            if (currentOutput.contains("Exception")) {
                portDetermined(null);
            }
            return;
        }
        portDetermined(Integer.valueOf(regexMatcher.group(1)));
    }

    private void portDetermined(Integer portValue) {
        this.port = portValue;
        try {
            portDetermined = true;
            sb = null;
            portDeterminedCondition.signal();
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected void handleError(IOException e) {
        if (!portDetermined) {
            portDetermined(null);
        }
    }

    public void awaitForPort() throws InterruptedException {
        lock.lock();
        try {
            while (!portDetermined) {
                portDeterminedCondition.await();
            }
        } finally {
            lock.unlock();
        }
    }

    public Integer getPort() {
        return port;
    }
}
