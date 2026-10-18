package io.quarkus.devservices.deployment;

import static org.testcontainers.containers.output.OutputFrame.OutputType.STDERR;
import static org.testcontainers.containers.output.OutputFrame.OutputType.STDOUT;

import java.io.Closeable;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.jboss.logging.Logger;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.output.FrameConsumerResultCallback;
import org.testcontainers.containers.output.OutputFrame;

import io.quarkus.deployment.dev.devservices.ContainerInfo;
import io.quarkus.deployment.dev.devservices.DevServiceDescriptionBuildItem;

public class ContainerLogForwarder implements Closeable {

    private final DevServiceDescriptionBuildItem devService;
    private final AtomicLong timestamp = new AtomicLong(0L);
    private final Logger logger;
    private FrameConsumerResultCallback resultCallback;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public ContainerLogForwarder(DevServiceDescriptionBuildItem devService) {
        this.devService = devService;
        this.logger = Logger.getLogger(devService.getName());
    }

    public DevServiceDescriptionBuildItem getDevService() {
        return devService;
    }

    public boolean isRunning() {
        return running.get();
    }

    public void start() {
        ContainerInfo containerInfo = devService.getContainerInfo();

        if (containerInfo != null) {
            String shortId = containerInfo.getShortId();

            if (running.compareAndSet(false, true)) {
                this.resultCallback = new FrameConsumerResultCallback();
                this.resultCallback.addConsumer(STDOUT, frame -> {
                    if (running.get())
                        logger.infof("[%s] %s", shortId, updateTimestamp(frame));
                });
                this.resultCallback.addConsumer(STDERR, frame -> {
                    if (running.get())
                        logger.errorf("[%s] %s", shortId, updateTimestamp(frame));
                });
                DockerClientFactory.lazyClient().logContainerCmd(containerInfo.id())
                        .withFollowStream(true)
                        .withStdErr(true)
                        .withStdOut(true)
                        .withSince(timestamp.intValue())
                        .exec(resultCallback);
            }
        }
    }

    @Override
    public void close() {
        if (running.compareAndSet(true, false)) {
            try {
                resultCallback.close();
            } catch (Throwable throwable) {
                logger.errorf("Failed to close log forwarder %s", devService.getName());
            } finally {
                resultCallback = null;
            }
        }
    }

    private String updateTimestamp(OutputFrame frame) {
        timestamp.set(Instant.now().getEpochSecond());
        return frame.getUtf8String().trim();
    }

}
