
package io.quarkus.kubernetes.deployment;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.jkube.kit.common.util.OpenshiftHelper;
import org.eclipse.jkube.kit.config.service.ApplyService;
import org.jboss.logging.Logger;

import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.kubernetes.spi.KubernetesImageBuildItem;
import io.quarkus.kubernetes.spi.KubernetesManifestBuildItem;

public class KubernetesApplyProcessor extends AbstractJKubeProcessor {

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

    private static <T extends KubernetesClient> T initClient(JKubeLogger log) {
        return initClusterAccess().createDefaultClient(log);
    }

    @BuildStep(onlyIf = IsNormal.class)
    public List<ArtifactResultBuildItem> apply(
            KubernetesConfig kubernetesConfig,
            OutputTargetBuildItem outputTargetBuildItem,
            List<KubernetesManifestBuildItem> generatedManifests,
            Optional<KubernetesImageBuildItem> kubernetesImageBuildItem) {

        if (!kubernetesImageBuildItem.isPresent() || kubernetesConfig.skipApply) {
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
}
