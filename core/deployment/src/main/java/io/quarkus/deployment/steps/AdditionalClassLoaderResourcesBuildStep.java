package io.quarkus.deployment.steps;

import static io.quarkus.commons.classloading.ClassLoaderHelper.fromClassNameToResourceName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AdditionalClassLoaderResourcesBuildItem;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;

public class AdditionalClassLoaderResourcesBuildStep {

    @BuildStep
    void appendAdditionalClassloaderResources(BuildProducer<AdditionalIndexedClassesBuildItem> producer,
            List<AdditionalClassLoaderResourcesBuildItem> additionalResources) {

        if (!additionalResources.isEmpty()) {
            QuarkusClassLoader cl = (QuarkusClassLoader) Thread.currentThread().getContextClassLoader();

            Map<String, byte[]> collected = new LinkedHashMap<String, byte[]>();
            List<String> additionalClassesToIndex = new ArrayList<String>();
            for (AdditionalClassLoaderResourcesBuildItem item : additionalResources) {

                for (Entry<String, byte[]> entry : item.getResources().entrySet()) {
                    additionalClassesToIndex.add(entry.getKey());

                    collected.put(entry.getKey(), entry.getValue());
                    // add it also as resources to allow index to work properly
                    collected.put(fromClassNameToResourceName(entry.getKey()), entry.getValue());

                }
            }

            cl.reset(collected, Collections.emptyMap());
            // produce the AdditionalIndexedClassesBuildItem so this build step
            // is actually invoked and allow to directly index all the classes
            producer.produce(new AdditionalIndexedClassesBuildItem(additionalClassesToIndex.stream().toArray(String[]::new)));
        }
    }
}
