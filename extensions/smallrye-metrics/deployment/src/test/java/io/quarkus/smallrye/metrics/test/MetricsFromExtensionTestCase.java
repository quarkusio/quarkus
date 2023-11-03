package io.quarkus.smallrye.metrics.test;

import static org.hamcrest.MatcherAssert.assertThat;

import java.util.function.Consumer;

import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.hamcrest.Matchers;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.MethodInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.smallrye.metrics.deployment.spi.MetricBuildItem;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

/**
 * Test the metric registration mechanism for other Quarkus extensions.
 * The test simulates an extension that registers a counter in VENDOR registry for every method
 * whose name starts with "countMePlease", and a counter in BASE registry for every method
 * whose name starts with "countMeInBaseScope"
 */
public class MetricsFromExtensionTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MetricResource.class))
            .addBuildChainCustomizer(buildCustomizer());

    protected static Consumer<BuildChainBuilder> buildCustomizer() {
        return new Consumer<BuildChainBuilder>() {
            // This represents the extension.
            @Override
            public void accept(BuildChainBuilder builder) {
                builder.addBuildStep(context -> {
                    BeanArchiveIndexBuildItem indexBuildItem = context.consume(BeanArchiveIndexBuildItem.class);
                    for (ClassInfo clazz : indexBuildItem.getIndex().getKnownClasses()) {
                        for (MethodInfo method : clazz.methods()) {
                            if (method.name().startsWith("countMePlease")) {
                                Metadata metricMetadata = Metadata.builder()
                                        .withType(MetricType.COUNTER)
                                        .withName(clazz.name().toString() + "." + method.name())
                                        .build();
                                MetricBuildItem buildItem = new MetricBuildItem.Builder()
                                        .metadata(metricMetadata)
                                        .enabled(true)
                                        .build();
                                context.produce(buildItem);
                            } else if (method.name().startsWith("countMeInBaseScope")) {
                                Metadata metricMetadata = Metadata.builder()
                                        .withType(MetricType.COUNTER)
                                        .withName(clazz.name().toString() + "." + method.name())
                                        .build();
                                MetricBuildItem buildItem = new MetricBuildItem.Builder()
                                        .metadata(metricMetadata)
                                        .registryType(MetricRegistry.Type.BASE)
                                        .enabled(true)
                                        .build();
                                context.produce(buildItem);
                            }
                        }
                    }
                }).consumes(BeanArchiveIndexBuildItem.class)
                        .produces(MetricBuildItem.class)
                        .build();
            }
        };
    }

    @Test
    public void testVendorRegistryType() {
        String[] metricNames = RestAssured.when().get("/get-counters").then().extract().as(String[].class);
        assertThat(metricNames, Matchers.hasItemInArray("io.quarkus.smallrye.metrics.test.MetricResource.countMePlease"));
        assertThat(metricNames, Matchers.hasItemInArray("io.quarkus.smallrye.metrics.test.MetricResource.countMePlease2"));
    }

    @Test
    public void testBaseRegistryType() {
        String[] metricNames = RestAssured.when().get("/get-counters-base").then().extract().as(String[].class);
        assertThat(metricNames, Matchers.hasItemInArray("io.quarkus.smallrye.metrics.test.MetricResource.countMeInBaseScope"));
    }

}
