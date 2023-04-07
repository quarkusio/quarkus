package io.quarkus.devui.deployment.menu;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devui.deployment.InternalPageBuildItem;
import io.quarkus.devui.spi.page.Page;

/**
 * This creates Continuous Testing Page
 */
public class ContinuousTestingProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    InternalPageBuildItem createContinuousTestingPages() {

        InternalPageBuildItem continuousTestingPages = new InternalPageBuildItem("Continuous Testing", 30);

        continuousTestingPages.addPage(Page.webComponentPageBuilder()
                .namespace("devui-continuous-testing")
                .title("Continuous Testing")
                .icon("font-awesome-solid:flask-vial")
                .componentLink("qwc-continuous-testing.js"));

        return continuousTestingPages;

    }

}