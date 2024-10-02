package io.quarkus.qute.deployment;

import java.util.function.Predicate;

import org.jboss.jandex.DotName;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.qute.deployment.TypeCheckExcludeBuildItem.TypeCheck;

public class JsonObjectProcessor {

    @BuildStep
    void init(Capabilities capabilities, BuildProducer<AdditionalBeanBuildItem> beans,
            BuildProducer<TypeCheckExcludeBuildItem> typeCheckExcludes) {
        if (capabilities.isPresent(Capability.VERTX)) {
            beans.produce(new AdditionalBeanBuildItem("io.quarkus.qute.runtime.jsonobject.JsonObjectValueResolver"));
            DotName jsonObjectName = DotName.createSimple("io.vertx.core.json.JsonObject");
            typeCheckExcludes.produce(new TypeCheckExcludeBuildItem(new Predicate<TypeCheckExcludeBuildItem.TypeCheck>() {
                @Override
                public boolean test(TypeCheck tc) {
                    return tc.classNameEquals(jsonObjectName);
                }
            }));
        }
    }
}
