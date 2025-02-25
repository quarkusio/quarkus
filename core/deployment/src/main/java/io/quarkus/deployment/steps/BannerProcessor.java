package io.quarkus.deployment.steps;

import static io.quarkus.commons.classloading.ClassLoaderHelper.fromClassNameToResourceName;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.Enumeration;
import java.util.Map;
import java.util.Scanner;

import org.jboss.logging.Logger;

import io.quarkus.banner.BannerConfig;
import io.quarkus.builder.Version;
import io.quarkus.deployment.IsTest;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ConsoleFormatterBannerBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.util.FileUtil;
import io.quarkus.runtime.BannerRecorder;
import io.quarkus.runtime.util.ClassPathUtils;

public class BannerProcessor {

    private static final Logger logger = Logger.getLogger(BannerProcessor.class);

    @BuildStep(onlyIfNot = { IsTest.class })
    @Record(ExecutionTime.RUNTIME_INIT)
    public ConsoleFormatterBannerBuildItem recordBanner(BannerRecorder recorder, BannerConfig config) {
        String bannerText = readBannerFile(config);
        return new ConsoleFormatterBannerBuildItem(recorder.provideBannerSupplier(bannerText));
    }

    @BuildStep
    HotDeploymentWatchedFileBuildItem watchBannerChanges(BannerConfig config) {
        return new HotDeploymentWatchedFileBuildItem(config.path());
    }

    private String readBannerFile(BannerConfig config) {
        try {
            Map.Entry<URL, Boolean> entry = getBanner(config);
            URL bannerResourceURL = entry.getKey();
            if (bannerResourceURL == null) {
                logger.warn("Could not locate banner file");
                return "";
            }
            return ClassPathUtils.readStream(bannerResourceURL, is -> {
                try {
                    byte[] content = FileUtil.readFileContents(is);
                    String bannerTitle = new String(content, StandardCharsets.UTF_8);

                    int width = 0;
                    try (Scanner scanner = new Scanner(bannerTitle)) {
                        while (scanner.hasNextLine()) {
                            width = Math.max(width, scanner.nextLine().length());
                        }
                    }

                    String tagline = "\n";
                    Boolean isDefaultBanner = entry.getValue();
                    if (!isDefaultBanner) {
                        tagline = String.format("\n%" + width + "s\n", "Powered by Quarkus " + Version.getVersion());
                    }

                    return bannerTitle + tagline;
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (IOException e) {
            logger.warn("Unable to read banner file");
            return "";
        }
    }

    /**
     * @return an entry containing the text of the banner as the key and whether the default banner is being used as the
     *         value. The default banner is used as a last report
     */
    private Map.Entry<URL, Boolean> getBanner(BannerConfig config) throws IOException {
        Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources(config.path());
        URL defaultBanner = null;
        URL firstNonDefaultBanner = null;
        while (resources.hasMoreElements() && firstNonDefaultBanner == null) {
            URL url = resources.nextElement();
            if (defaultBanner == null && isQuarkusCoreBanner(url)) {
                defaultBanner = url;
            } else {
                firstNonDefaultBanner = url;
            }
        }
        if (firstNonDefaultBanner == null) {
            return new AbstractMap.SimpleEntry<>(defaultBanner, true);
        } else {
            return new AbstractMap.SimpleEntry<>(firstNonDefaultBanner, false);
        }
    }

    protected boolean isQuarkusCoreBanner(URL url) {
        if (!"jar".equals(url.getProtocol())) {
            return false;
        }

        String thisClassName = this.getClass().getName();

        try {
            return ClassPathUtils.processAsPath(url, p -> {
                // We determine whether the banner is the default by checking to see if the jar that contains it also
                // contains this class. This way although somewhat complicated guarantees that any rename of artifacts
                // won't affect the check
                Path resolved = p.resolve("/" + fromClassNameToResourceName(thisClassName));
                return Files.exists(resolved);
            });
        } catch (UncheckedIOException ex) {
            return false;
        }
    }
}
