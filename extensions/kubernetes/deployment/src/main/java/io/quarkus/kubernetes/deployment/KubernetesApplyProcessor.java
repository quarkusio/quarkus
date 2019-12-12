
package io.quarkus.kubernetes.deployment;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.OpenshiftHelper;
import org.eclipse.jkube.kit.config.access.ClusterAccess;
import org.eclipse.jkube.kit.config.access.ClusterConfiguration;
import org.eclipse.jkube.kit.config.service.ApplyService;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.JarBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.kubernetes.spi.KubernetesManifestBuildItem;

public class KubernetesApplyProcessor {

    private static final Logger log = Logger.getLogger(KubernetesApplyProcessor.class);

    static List<Path> getApplicableManifests(String target, Path outputDirectory,
            List<KubernetesManifestBuildItem> generatedManifests) {

        return generatedManifests
                .stream()
                .filter(mf -> mf.getTarget().equals(target))
                .map(KubernetesManifestBuildItem::getPath)
                .filter(Objects::nonNull)
                .map(outputDirectory::resolve)
                .filter(p -> p.toFile().exists())
                .filter(p -> p.toFile().isFile())
                .filter(p -> p.toFile().getName().matches(".+\\.ya?ml$"))
                .collect(Collectors.toList());
    }

    private static <T extends KubernetesClient> String getTarget(T kubernetesClient) {
        return OpenshiftHelper.isOpenShift(kubernetesClient) ? "openshift" : "kubernetes";
    }

    private static ClusterConfiguration getClusterConfiguration() {
        final Config config = ConfigProvider.getConfig();
        final Properties props = new Properties();
        StreamSupport.stream(config.getPropertyNames().spliterator(), false)
                .forEach(key -> props.put(key, config.getValue(key, String.class)));
        return new ClusterConfiguration.Builder().from(System.getProperties()).from(props).build();
    }

    private static <T extends KubernetesClient> T initClient(JKubeLogger log) {
        return new ClusterAccess(getClusterConfiguration()).createDefaultClient(log);
    }

    @BuildStep(onlyIf = IsNormal.class)
    public List<ArtifactResultBuildItem> apply(
            KubernetesConfig kubernetesConfig,
            OutputTargetBuildItem outputTargetBuildItem,
            List<KubernetesManifestBuildItem> generatedManifests,
            JarBuildItem jarBuildItem) {

        if (kubernetesConfig.skipApply) {
            log.info("Kubernetes apply phase is skipped");
            return Collections.emptyList();
        }

        final JKubeLogger jKubeLogger = new JKubeLogger(log);
        final KubernetesClient kubernetesClient = initClient(jKubeLogger);
        final ApplyService applyService = new ApplyService(kubernetesClient, jKubeLogger);
        final List<Path> generatedManifestPaths = getApplicableManifests(
                getTarget(kubernetesClient), outputTargetBuildItem.getOutputDirectory(), generatedManifests);
        generatedManifestPaths.forEach(manifestPath -> {
            final File manifestFile = manifestPath.toFile();
            try (FileInputStream fis = new FileInputStream(manifestFile)) {
                applyService.applyList(Serialization.unmarshal(fis, KubernetesList.class), manifestFile.getName());
            } catch (Exception ex) {
                log.errorf(ex, "Error parsing manifest %s", manifestFile.getName());
            }
        });
        return generatedManifestPaths.stream()
                .map(mf -> new ArtifactResultBuildItem(mf, "yml", Collections.emptyMap()))
                .collect(Collectors.toList());
    }

    private static final class JKubeLogger implements KitLogger {

        private final Logger log;

        private JKubeLogger(Logger log) {
            this.log = log;
        }

        @Override
        public void debug(String format, Object... params) {
            log.debugf(format, params);
        }

        @Override
        public void info(String format, Object... params) {
            log.infof(format, params);
        }

        @Override
        public void warn(String format, Object... params) {
            log.warnf(format, params);
        }

        @Override
        public void error(String format, Object... params) {
            log.errorf(format, params);
        }

        @Override
        public boolean isDebugEnabled() {
            return log.isDebugEnabled();
        }
    }
}
