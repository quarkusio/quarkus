package io.quarkus.deployment.steps;

import static io.quarkus.commons.classloading.ClassloadHelper.fromClassNameToResourceName;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.Base64;
import java.util.Enumeration;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Scanner;

import javax.imageio.ImageIO;

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
        String graphicalBannerText = readGraphicalBannerFile(config);
        return new ConsoleFormatterBannerBuildItem(recorder.provideBannerSupplier(bannerText, graphicalBannerText));
    }

    @BuildStep
    HotDeploymentWatchedFileBuildItem watchBannerChanges(BannerConfig config) {
        return new HotDeploymentWatchedFileBuildItem(config.path);
    }

    private String readBannerFile(BannerConfig config) {
        try {
            Map.Entry<URL, Boolean> entry = getBanner(config, config.path);
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

    private String readGraphicalBannerFile(BannerConfig config) {
        try {
            Map.Entry<URL, Boolean> entry = getBanner(config, config.image.path);
            URL bannerResourceURL = entry.getKey();
            if (bannerResourceURL == null) {
                logger.warn("Could not locate graphical banner file");
                return "";
            }
            String filePart = bannerResourceURL.getFile();
            if (filePart != null && filePart.endsWith(".png")) {
                // graphical banner
                return ClassPathUtils.readStream(bannerResourceURL, is -> {
                    StringBuilder b = new StringBuilder(16384);
                    try (OutputStream os = Base64.getEncoder().wrap(new OutputStream() {
                        final byte[] buffer = new byte[2048];
                        int pos = 0;

                        public void write(final int b) {
                            if (pos == buffer.length)
                                more();
                            buffer[pos++] = (byte) b;
                        }

                        public void write(final byte[] b, int off, int len) {
                            while (len > 0) {
                                if (pos == buffer.length) {
                                    more();
                                }
                                final int cnt = Math.min(len, buffer.length - pos);
                                System.arraycopy(b, off, buffer, pos, cnt);
                                pos += cnt;
                                off += cnt;
                                len -= cnt;
                            }
                        }

                        void more() {
                            b.append("m=1;");
                            b.append(new String(buffer, 0, pos, StandardCharsets.US_ASCII));
                            b.append("\033\\");
                            // set up next segment
                            b.append("\033_G");
                            pos = 0;
                        }

                        public void close() {
                            b.append("m=0;");
                            b.append(new String(buffer, 0, pos, StandardCharsets.US_ASCII));
                            b.append("\033\\\n");
                            pos = 0;
                        }
                    })) {
                        byte[] bytes = is.readAllBytes();

                        OptionalInt rows = config.image.rows;
                        OptionalInt columns = config.image.columns;
                        // we can only force scale if exactly one of rows/columns is empty
                        if (config.image.forceScale && rows.isEmpty() != columns.isEmpty()) {
                            BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
                            int w = img.getWidth();
                            int h = img.getHeight();
                            float iar = Math.round((float) w / (float) h);
                            float far = config.image.fontAspectRatio;

                            if (rows.isEmpty()) {
                                assert columns.isPresent();
                                rows = OptionalInt.of(Math.round((float) columns.getAsInt() / (iar / far)));
                            } else {
                                assert rows.isPresent();
                                columns = OptionalInt.of(Math.round((float) rows.getAsInt() * (iar / far)));
                            }
                        }

                        // set the header
                        b.append("\033_Gf=100,a=T,");
                        rows.ifPresent(n -> b.append("r=").append(n).append(','));
                        columns.ifPresent(n -> b.append("c=").append(n).append(','));
                        // write the data in encoded chunks
                        os.write(bytes);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                    Boolean isDefaultBanner = entry.getValue();
                    if (!isDefaultBanner.booleanValue()) {
                        b.append("Powered by Quarkus ");
                        b.append(Version.getVersion());
                        b.append('\n');
                    }
                    return b.toString();
                });
            }
            return "";
        } catch (IOException e) {
            logger.warn("Unable to read banner file");
            return "";
        }
    }

    /**
     * @return an entry containing the text of the banner as the key and whether the default banner is being used as the
     *         value. The default banner is used as a last report
     */
    private Map.Entry<URL, Boolean> getBanner(BannerConfig config, String path) throws IOException {
        Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources(path);
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
