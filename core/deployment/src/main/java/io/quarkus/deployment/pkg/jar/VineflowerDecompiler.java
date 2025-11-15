package io.quarkus.deployment.pkg.jar;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jboss.logging.Logger;

import io.smallrye.common.process.ProcessBuilder;
import io.smallrye.common.process.ProcessUtil;

class VineflowerDecompiler implements Decompiler {

    private static final Logger LOG = Logger.getLogger(VineflowerDecompiler.class);
    private static final String DEFAULT_VINEFLOWER_VERSION = "1.11.1";

    private Context context;
    private Path decompilerJar;

    @Override
    public void init(Context context) {
        this.context = context.versionStr != null ? context
                : new Context(DEFAULT_VINEFLOWER_VERSION, context.jarLocation, context.decompiledOutputDir);
        this.decompilerJar = this.context.jarLocation.resolve(String.format("vineflower-%s.jar", this.context.versionStr));
    }

    @Override
    public boolean downloadIfNecessary() {
        if (Files.exists(decompilerJar)) {
            return true;
        }
        String downloadURL = String.format(
                "https://repo.maven.apache.org/maven2/org/vineflower/vineflower/%s/vineflower-%s.jar",
                context.versionStr, context.versionStr);
        try (BufferedInputStream in = new BufferedInputStream(new URL(downloadURL).openStream());
                OutputStream fileOutputStream = Files.newOutputStream(decompilerJar)) {
            in.transferTo(fileOutputStream);
            return true;
        } catch (IOException e) {
            LOG.error("Unable to download Vineflower from " + downloadURL, e);
            return false;
        }
    }

    @Override
    public boolean decompile(Path jarToDecompile) {
        String jarFileName = jarToDecompile.getFileName().toString();
        int dotIndex = jarFileName.indexOf('.');
        String outputDirectory = jarFileName.substring(0, dotIndex);
        var pb = ProcessBuilder.newBuilder(ProcessUtil.pathOfJava())
                .arguments(
                        "-jar",
                        decompilerJar.toAbsolutePath().toString(),
                        "-rsy=0", // synthetic methods
                        "-rbr=0", // bridge methods
                        jarToDecompile.toAbsolutePath().toString(),
                        context.decompiledOutputDir.resolve(outputDirectory).toAbsolutePath().toString());
        if (LOG.isDebugEnabled()) {
            pb.output().consumeLinesWith(8192, LOG::debug);
        }
        try {
            pb.run();
        } catch (Exception e) {
            LOG.error("Decompilation failed", e);
            return false;
        }
        return true;
    }
}
