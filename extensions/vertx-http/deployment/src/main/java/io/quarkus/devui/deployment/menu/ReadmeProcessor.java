package io.quarkus.devui.deployment.menu;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import io.quarkus.deployment.IsLocalDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devui.deployment.InternalPageBuildItem;
import io.quarkus.devui.runtime.readme.ReadmeJsonRPCService;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.Page;

/**
 * This creates Readme Page
 */
public class ReadmeProcessor {

    private static final String NS = "devui-readme";

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    void createReadmePage(BuildProducer<InternalPageBuildItem> internalPageProducer) {

        String readme = getContents("README.md")
                .orElse(getContents("readme.md")
                        .orElse(null));

        if (readme != null) {
            InternalPageBuildItem readmePage = new InternalPageBuildItem("Readme", 51);

            readmePage.addBuildTimeData("readme", readme);

            readmePage.addPage(Page.webComponentPageBuilder()
                    .namespace(NS)
                    .title("Readme")
                    .icon("font-awesome-brands:readme")
                    .componentLink("qwc-readme.js"));

            internalPageProducer.produce(readmePage);
        }
    }

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    JsonRPCProvidersBuildItem createJsonRPCServiceForCache() {
        return new JsonRPCProvidersBuildItem(NS, ReadmeJsonRPCService.class);
    }

    private Optional<String> getContents(String name) {
        Path p = Path.of(name);
        if (Files.exists(p)) {
            try {
                return Optional.of(Files.readString(p));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return Optional.empty();
    }
}
