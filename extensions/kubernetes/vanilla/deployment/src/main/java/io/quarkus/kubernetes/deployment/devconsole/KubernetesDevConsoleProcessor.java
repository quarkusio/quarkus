package io.quarkus.kubernetes.deployment.devconsole;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import io.quarkus.bootstrap.BootstrapException;
import io.quarkus.bootstrap.app.AugmentAction;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.builder.BuildResult;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.devconsole.spi.DevConsoleTemplateInfoBuildItem;
import io.quarkus.kubernetes.deployment.SelectedKubernetesDeploymentTargetBuildItem;
import io.quarkus.kubernetes.spi.GeneratedKubernetesResourceBuildItem;

public class KubernetesDevConsoleProcessor {

    static volatile List<Manifest> manifests;
    static final Holder holder = new Holder();

    @BuildStep
    void builder(BuildProducer<DevConsoleTemplateInfoBuildItem> infos) {
        manifests = null; // ensures that the manifests are re-generated when a live-load is performed
        infos.produce(new DevConsoleTemplateInfoBuildItem("holder", holder));
    }

    public static class GeneratedKubernetesResourceHandler implements BiConsumer<Map<String, byte[]>, BuildResult> {
        @Override
        public void accept(Map<String, byte[]> context, BuildResult buildResult) {
            // the idea here is to only display the content of the manifest file that will be selected for deployment
            var generatedFilesBI = buildResult
                    .consumeMulti(GeneratedKubernetesResourceBuildItem.class);
            var selectedTargetBI = buildResult
                    .consume(SelectedKubernetesDeploymentTargetBuildItem.class);
            for (var bi : generatedFilesBI) {
                if (bi.getName().startsWith(selectedTargetBI.getEntry().getName())
                        && bi.getName().endsWith(".yml")) {
                    context.put(bi.getName(), bi.getContent());
                }
            }
        }
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

    private static final class Manifest {
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
