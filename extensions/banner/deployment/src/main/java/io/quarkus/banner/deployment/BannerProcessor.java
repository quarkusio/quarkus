package io.quarkus.banner.deployment;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.jboss.logging.Logger;

import io.quarkus.banner.runtime.BannerRecorder;
import io.quarkus.builder.Version;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ConsoleFormatterBannerBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.util.FileUtil;

public class BannerProcessor {

    private static final Logger logger = Logger.getLogger(BannerProcessor.class);

    @BuildStep(loadsApplicationClasses = true, onlyIf = IsBanner.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    public ConsoleFormatterBannerBuildItem recordBanner(BannerRecorder recorder, BannerConfig config) {
        String bannerText = readBannerFile(config);
        return new ConsoleFormatterBannerBuildItem(recorder.provideBannerSupplier(bannerText));
    }

    @BuildStep
    HotDeploymentWatchedFileBuildItem watchBannerChanges(BannerConfig config) {
        return new HotDeploymentWatchedFileBuildItem(config.path);
    }

    private String readBannerFile(BannerConfig config) {
        URL resource = Thread.currentThread().getContextClassLoader().getResource(config.path);
        if (resource != null) {
            try (InputStream is = resource.openStream()) {
                byte[] content = FileUtil.readFileContents(is);
                String bannerTitle = new String(content, StandardCharsets.UTF_8);
                return bannerTitle + "\n:: Quarkus :: v" + Version.getVersion() + '\n';
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        } else {
            logger.warn("Could not read banner file");
            return "";
        }
    }
}
