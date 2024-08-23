package io.quarkus.resteasy.reactive.server.test.multipart;

import static org.awaitility.Awaitility.await;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

abstract class AbstractMultipartTest {

    protected boolean isDirectoryEmpty(Path uploadDir) {
        File[] files = uploadDir.toFile().listFiles();
        if (files == null) {
            return true;
        }
        return files.length == 0;
    }

    protected void clearDirectory(Path uploadDir) {
        File[] files = uploadDir.toFile().listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (!file.isDirectory()) {
                file.delete();
            }
        }
    }

    protected void awaitUploadDirectoryToEmpty(Path uploadDir) {
        await().atMost(10, TimeUnit.SECONDS)
                .pollInterval(Duration.ofSeconds(1))
                .until(new Callable<Boolean>() {
                    @Override
                    public Boolean call() {
                        return isDirectoryEmpty(uploadDir);
                    }
                });
    }

    protected String fileSizeAsStr(File file) throws IOException {
        return "" + Files.readAllBytes(file.toPath()).length;
    }
}
