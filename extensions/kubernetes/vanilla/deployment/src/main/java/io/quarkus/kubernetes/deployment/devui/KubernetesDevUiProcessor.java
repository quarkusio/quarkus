package io.quarkus.kubernetes.deployment.devui;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.kubernetes.deployment.devconsole.KubernetesDevConsoleProcessor;
import io.quarkus.kubernetes.runtime.devui.KubernetesManifestService;

public class KubernetesDevUiProcessor {

    static final KubernetesDevConsoleProcessor.Holder holder = new KubernetesDevConsoleProcessor.Holder();

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
    JsonRPCProvidersBuildItem createJsonRPCServiceForCache() {
        DevConsoleManager.register("kubernetes-generate-manifest", ignored -> {
            System.out.println("calling action");
            try {
                List<KubernetesDevConsoleProcessor.Manifest> manifests = holder.getManifests();
                // Avoid relying on databind.
                Map<String, String> map = new LinkedHashMap<>();
                System.out.println("manifests are " + manifests);
                for (KubernetesDevConsoleProcessor.Manifest manifest : manifests) {
                    map.put(manifest.getName(), manifest.getContent());
                    System.out.println("added " + manifest.getName());
                }
                return map;
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        });
        return new JsonRPCProvidersBuildItem(KubernetesManifestService.class);
    }
}
