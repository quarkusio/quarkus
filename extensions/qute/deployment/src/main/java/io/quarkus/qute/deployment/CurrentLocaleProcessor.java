package io.quarkus.qute.deployment;

import org.jboss.jandex.DotName;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;

public class CurrentLocaleProcessor {

    private static final DotName CURRENT_LOCALE_PROVIDER = DotName.createSimple("io.quarkus.qute.i18n.CurrentLocaleProvider");

    @BuildStep
    void init(Capabilities capabilities, BuildProducer<AdditionalBeanBuildItem> beans,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans) {
        // Make sure all CurrentLocaleProvider implementations are unremovable
        unremovableBeans.produce(UnremovableBeanBuildItem.beanTypes(CURRENT_LOCALE_PROVIDER));

        if (capabilities.isPresent(Capability.VERTX)) {
            // The default CurrentLocaleProvider resolves the current locale from the Accept-Language header of the
            // current HTTP request; it needs the current io.vertx.core.http.HttpServerRequest and so it's only
            // registered if the Vert.x capability is present
            beans.produce(new AdditionalBeanBuildItem("io.quarkus.qute.runtime.i18n.HttpServerRequestLocaleProvider"));
        }
    }

}
