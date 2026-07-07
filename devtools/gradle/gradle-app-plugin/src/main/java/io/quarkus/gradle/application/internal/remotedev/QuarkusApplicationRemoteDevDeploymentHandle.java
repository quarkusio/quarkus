package io.quarkus.gradle.application.internal.remotedev;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import javax.inject.Inject;

import org.gradle.deployment.internal.Deployment;
import org.gradle.deployment.internal.DeploymentHandle;

public class QuarkusApplicationRemoteDevDeploymentHandle implements DeploymentHandle {

    private final Path closeReceiptFile;
    private final Path reconnectTriggerFile;
    private final String reconnectEpoch;
    private QuarkusApplicationRemoteDevSession session;
    private boolean running;
    private boolean stopped;

    @Inject
    public QuarkusApplicationRemoteDevDeploymentHandle(Path closeReceiptFile, Path reconnectTriggerFile,
            String reconnectEpoch) {
        this.closeReceiptFile = closeReceiptFile;
        this.reconnectTriggerFile = reconnectTriggerFile;
        this.reconnectEpoch = UUID.fromString(reconnectEpoch).toString();
    }

    @Override
    public synchronized boolean isRunning() {
        return running;
    }

    @Override
    public synchronized void start(Deployment deployment) {
        if (stopped) {
            throw new IllegalStateException("Stopped Quarkus remote-dev deployment cannot be restarted");
        }
        running = true;
    }

    public synchronized QuarkusApplicationRemoteDevSession session() {
        if (!running || stopped) {
            throw new IllegalStateException("Quarkus remote-dev deployment is not running");
        }
        if (session == null) {
            session = new QuarkusApplicationRemoteDevSession(reconnectTriggerFile, reconnectEpoch);
        }
        return session;
    }

    @Override
    public void stop() {
        QuarkusApplicationRemoteDevSession sessionToClose;
        synchronized (this) {
            if (stopped) {
                return;
            }
            running = false;
            stopped = true;
            sessionToClose = session;
            session = null;
        }
        Exception failure = null;
        if (sessionToClose != null) {
            try {
                sessionToClose.close();
            } catch (IOException e) {
                failure = e;
            }
        }
        try {
            writeCloseReceipt();
        } catch (IOException e) {
            if (failure == null) {
                failure = e;
            } else {
                failure.addSuppressed(e);
            }
        }
        if (failure != null) {
            throw new IllegalStateException("Failed to stop Quarkus remote dev", failure);
        }
    }

    private void writeCloseReceipt() throws IOException {
        Files.createDirectories(closeReceiptFile.getParent());
        Files.writeString(closeReceiptFile, "closed\n", StandardCharsets.UTF_8);
    }
}
