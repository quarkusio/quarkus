package io.quarkus.resteasy.mutiny.deployment;

import org.jboss.logging.Logger;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

public class ResteasyMutinyProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        warn();
        return new FeatureBuildItem(Feature.RESTEASY_MUTINY);
    }

    void warn() {
        Logger.getLogger(ResteasyMutinyProcessor.class).warn("The quarkus-resteasy-mutiny extension is deprecated. " +
                "Switch to Quarkus REST instead.\n" +
                "This extension adds support for Uni and Multi to RESTEasy Classic, without using the reactive execution model,"
                +
                " as RESTEasy Classic does not use it. To properly integrate Mutiny and RESTEasy, use Quarkus REST. See https://quarkus.io/guides/getting-started-reactive for detailed instructions");
    }

}
