package io.quarkus.vertx.http.deployment;

import java.util.List;
import java.util.Optional;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;

/**
 * Registers the certificates configured through the legacy {@code quarkus.http.ssl.certificate.*} and
 * {@code quarkus.management.ssl.certificate.*} properties as native image resources, when they are
 * served from the classpath.
 * <p>
 * {@link io.quarkus.vertx.http.runtime.options.HttpServerOptionsUtils} looks these files up on the
 * classpath first and only falls back to the file system if that fails. Without registering them, the
 * classpath lookup returns {@code null} in native mode, the file system fallback does not find them
 * either, and the application fails to start with a {@code NoSuchFileException}.
 */
public class SslCertificateResourcesProcessor {

    private static final List<String> CERTIFICATE_PREFIXES = List.of(
            "quarkus.http.ssl.certificate.",
            "quarkus.management.ssl.certificate.");

    private static final List<String> CERTIFICATE_PROPERTIES = List.of(
            "files",
            "key-files",
            "key-store-file",
            "trust-store-file",
            "trust-store-files");

    @BuildStep
    void registerCertificatesAsNativeImageResources(BuildProducer<NativeImageResourceBuildItem> resources) {
        Config config = ConfigProvider.getConfig();
        for (String prefix : CERTIFICATE_PREFIXES) {
            for (String property : CERTIFICATE_PROPERTIES) {
                Optional<List<String>> values = config.getOptionalValues(prefix + property, String.class);
                if (values.isEmpty()) {
                    continue;
                }
                for (String value : values.get()) {
                    if (isOnClassPath(value)) {
                        resources.produce(new NativeImageResourceBuildItem(value));
                    }
                }
            }
        }
    }

    private static boolean isOnClassPath(String path) {
        return Thread.currentThread().getContextClassLoader().getResource(path) != null;
    }
}
