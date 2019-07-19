package io.quarkus.camel.component.salesforce.deployment;

import org.apache.camel.component.salesforce.SalesforceEndpointConfig;
import org.apache.camel.component.salesforce.SalesforceLoginConfig;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.JniBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;

class CamelSalesforceProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FeatureBuildItem.CAMEL_SALESFORCE);
    }

    @BuildStep
    JniBuildItem jni() {
        return new JniBuildItem();
    }

    @BuildStep
    ReflectiveClassBuildItem registerForReflection() {
        return new ReflectiveClassBuildItem(true, false,
                SalesforceLoginConfig.class,
                SalesforceEndpointConfig.class);
    }
}
