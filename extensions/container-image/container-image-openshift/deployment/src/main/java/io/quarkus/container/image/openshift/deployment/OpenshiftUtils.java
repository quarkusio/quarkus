package io.quarkus.container.image.openshift.deployment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import io.dekorate.kubernetes.decorator.Decorator;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ObjectReference;
import io.fabric8.openshift.api.model.ImageStreamTag;
import io.fabric8.openshift.api.model.SourceBuildStrategyFluent;
import io.fabric8.openshift.client.OpenShiftClient;

/**
 * This class is copied from Dekorate, with the difference that the {@code waitForImageStreamTags} method
 * take a client as the argument
 *
 * TODO: Update dekorate to take the client as an argument and then remove this class
 */
public class OpenshiftUtils {

    /**
     * Wait for the references ImageStreamTags to become available.
     *
     * @param client The openshift client used to check the status of the ImageStream
     * @param items A list of items, possibly referencing image stream tags.
     * @param amount The max amount of time to wait.
     * @param timeUnit The time unit of the time to wait.
     * @return True if the items became available false otherwise.
     */
    public static boolean waitForImageStreamTags(OpenShiftClient client, Collection<HasMetadata> items, long amount,
            TimeUnit timeUnit) {
        if (items == null || items.isEmpty()) {
            return true;
        }
        final List<String> tags = new ArrayList<>();
        new KubernetesListBuilder()
                .withItems(new ArrayList<>(items))
                .accept(new Decorator<SourceBuildStrategyFluent>() {
                    @Override
                    public void visit(SourceBuildStrategyFluent strategy) {
                        ObjectReference from = strategy.buildFrom();
                        if (from.getKind().equals("ImageStreamTag")) {
                            tags.add(from.getName());
                        }
                    }
                }).build();

        boolean tagsMissing = true;
        long started = System.currentTimeMillis();
        long elapsed = 0;

        while (tagsMissing && elapsed < timeUnit.toMillis(amount) && !Thread.interrupted()) {
            tagsMissing = false;
            for (String tag : tags) {
                ImageStreamTag t = client.imageStreamTags().withName(tag).get();
                if (t == null) {
                    tagsMissing = true;
                }
            }

            if (tagsMissing) {
                try {
                    Thread.sleep(1000);
                    elapsed = System.currentTimeMillis() - started;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        return !tagsMissing;
    }

    /**
     * Merges {@link OpenshiftConfig} with {@link S2iConfig} prioritizing in the former.
     * 
     * @param openshiftConfig the Openshift config
     * @param s2iConfig the s2i config
     * @param an instance of {@link OpenshiftConfig} with the merged configuration.
     */
    public static OpenshiftConfig mergeConfig(OpenshiftConfig openshiftConfig, S2iConfig s2iConfig) {
        OpenshiftConfig result = openshiftConfig != null ? openshiftConfig : new OpenshiftConfig();
        if (s2iConfig == null) {
            return result;
        }

        Config config = ConfigProvider.getConfig();
        Set<String> properties = StreamSupport.stream(config.getPropertyNames().spliterator(), false)
                .filter(s -> s.startsWith("quarkus.s2i.") || s.startsWith("quarkus.openshift."))
                .collect(Collectors.toSet());

        boolean hasS2iBaseJvmImage = properties.contains("quarkus.s2i.base-jvm-image");
        boolean hasS2iBaseNativeImage = properties.contains("quarkus.s2i.base-native-image");
        boolean hasS2iJvmArguments = properties.contains("quarkus.s2i.jvm-arguments");
        boolean hasS2iNativeArguments = properties.contains("quarkus.s2i.native-arguments");
        boolean hasS2iJarDirectory = properties.contains("quarkus.s2i.jar-directory");
        boolean hasS2iJarFileName = properties.contains("quarkus.s2i.jar-file-name");
        boolean hasS2iNativeBinaryDirectory = properties.contains("quarkus.s2i.native-binary-directory");
        boolean hasS2iNativeBinaryFileName = properties.contains("quarkus.s2i.native-binary-file-name");
        boolean hasS2iBuildTimeout = properties.contains("quarkus.s2i.native-binary-file-name");

        boolean hasOpenshiftBaseJvmImage = properties.contains("quarkus.openshift.base-jvm-image");
        boolean hasOpenshiftBaseNativeImage = properties.contains("quarkus.openshift.base-native-image");
        boolean hasOpenshiftJvmArguments = properties.contains("quarkus.openshift.jvm-arguments");
        boolean hasOpenshiftNativeArguments = properties.contains("quarkus.openshift.native-arguments");
        boolean hasOpenshiftJarDirectory = properties.contains("quarkus.openshift.jar-directory");
        boolean hasOpenshiftJarFileName = properties.contains("quarkus.openshift.jar-file-name");
        boolean hasOpenshiftNativeBinaryDirectory = properties.contains("quarkus.openshift.native-binary-directory");
        boolean hasOpenshiftNativeBinaryFileName = properties.contains("quarkus.openshift.native-binary-file-name");
        boolean hasOpenshiftBuildTimeout = properties.contains("quarkus.openshift.native-binary-file-name");

        result.baseJvmImage = hasS2iBaseJvmImage && !hasOpenshiftBaseJvmImage ? s2iConfig.baseJvmImage
                : openshiftConfig.baseJvmImage;
        result.baseNativeImage = hasS2iBaseNativeImage && !hasOpenshiftBaseNativeImage ? s2iConfig.baseNativeImage
                : openshiftConfig.baseNativeImage;
        result.jvmArguments = hasS2iJvmArguments && !hasOpenshiftJvmArguments ? s2iConfig.jvmArguments
                : openshiftConfig.jvmArguments;
        result.nativeArguments = hasS2iNativeArguments && !hasOpenshiftNativeArguments ? s2iConfig.nativeArguments
                : openshiftConfig.nativeArguments;
        result.jarDirectory = hasS2iJarDirectory && !hasOpenshiftJarDirectory ? Optional.of(s2iConfig.jarDirectory)
                : openshiftConfig.jarDirectory;
        result.jarFileName = hasS2iJarFileName && !hasOpenshiftJarFileName ? s2iConfig.jarFileName
                : openshiftConfig.jarFileName;
        result.nativeBinaryDirectory = hasS2iNativeBinaryDirectory && !hasOpenshiftNativeBinaryDirectory
                ? Optional.of(s2iConfig.nativeBinaryDirectory)
                : openshiftConfig.nativeBinaryDirectory;
        result.nativeBinaryFileName = hasS2iNativeBinaryFileName && !hasOpenshiftNativeBinaryFileName
                ? s2iConfig.nativeBinaryFileName
                : openshiftConfig.nativeBinaryFileName;
        result.buildTimeout = hasS2iBuildTimeout && !hasOpenshiftBuildTimeout ? s2iConfig.buildTimeout
                : openshiftConfig.buildTimeout;
        result.buildStrategy = openshiftConfig.buildStrategy;

        return result;
    }
}
