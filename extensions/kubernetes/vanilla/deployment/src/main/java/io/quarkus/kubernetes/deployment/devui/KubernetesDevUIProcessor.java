package io.quarkus.kubernetes.deployment.devui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import io.quarkus.bootstrap.BootstrapException;
import io.quarkus.bootstrap.app.AugmentAction;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.builder.BuildResult;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.devui.spi.buildtime.BuildTimeActionBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.kubernetes.deployment.SelectedKubernetesDeploymentTargetBuildItem;
import io.quarkus.kubernetes.spi.GeneratedKubernetesResourceBuildItem;

public class KubernetesDevUIProcessor {
    static volatile List<Manifest> manifests;
    static final Holder holder = new Holder();

    @BuildStep(onlyIf = IsDevelopment.class)
    CardPageBuildItem create(CurateOutcomeBuildItem bi) {
        CardPageBuildItem pageBuildItem = new CardPageBuildItem();
        pageBuildItem.addPage(Page.webComponentPageBuilder()
                .title("Kubernetes Manifests")
                .componentLink("qwc-kubernetes-manifest.js")
                .icon("font-awesome-solid:rocket"));

        return pageBuildItem;
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    BuildTimeActionBuildItem createBuildTimeActions() {
        BuildTimeActionBuildItem generateManifestActions = new BuildTimeActionBuildItem();
        generateManifestActions.addAction("generateManifests", ignored -> {
            try {
                List<Manifest> manifests = holder.getManifests();
                // Avoid relying on databind.
                Map<String, String> map = new LinkedHashMap<>();
                for (Manifest manifest : manifests) {
                    map.put(manifest.getName(), manifest.getContent());
                }
                return map;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        return generateManifestActions;
    }

    public static final class Holder {

        public List<Manifest> getManifests() throws BootstrapException {
            if (manifests == null) {
                synchronized (Holder.class) {
                    if (manifests == null) {
                        manifests = new ArrayList<>();
                        QuarkusBootstrap existing = (QuarkusBootstrap) DevConsoleManager.getQuarkusBootstrap();
                        QuarkusBootstrap quarkusBootstrap = existing.clonedBuilder()
                                .setMode(QuarkusBootstrap.Mode.PROD)
                                .setIsolateDeployment(true).build();
                        try (CuratedApplication bootstrap = quarkusBootstrap.bootstrap()) {
                            AugmentAction augmentor = bootstrap.createAugmentor();
                            Map<String, byte[]> context = new HashMap<>();
                            augmentor.performCustomBuild(GeneratedKubernetesResourceHandler.class.getName(), context,
                                    GeneratedKubernetesResourceBuildItem.class.getName(),
                                    SelectedKubernetesDeploymentTargetBuildItem.class.getName());
                            for (var entry : context.entrySet()) {
                                manifests.add(new Manifest(entry.getKey(), new String(entry.getValue())));
                            }
                        }
                    }
                }
            }
            return manifests;
        }
    }

    public static class GeneratedKubernetesResourceHandler implements BiConsumer<Map<String, byte[]>, BuildResult> {
        @Override
        public void accept(Map<String, byte[]> context, BuildResult buildResult) {
            // the idea here is to only display the content of the manifest file that will be selected for deployment
            var selectedTargetBI = buildResult
                    .consumeOptional(SelectedKubernetesDeploymentTargetBuildItem.class);
            if (selectedTargetBI == null) {
                return;
            }

            var generatedFilesBI = buildResult
                    .consumeMulti(GeneratedKubernetesResourceBuildItem.class);
            for (var bi : generatedFilesBI) {
                if (bi.getName().startsWith(selectedTargetBI.getEntry().getName())
                        && bi.getName().endsWith(".yml")) {
                    context.put(bi.getName(), bi.getContent());
                }
            }
        }
    }

    public static final class Manifest {
        private final String name;
        private final String content;

        public Manifest(String name, String content) {
            this.name = name;
            this.content = content;
        }

        public String getName() {
            return name;
        }

        public String getContent() {
            return content;
        }
    }
}
