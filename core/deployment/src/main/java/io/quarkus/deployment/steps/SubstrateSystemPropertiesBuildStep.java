package io.quarkus.deployment.steps;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ArchiveRootBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateOutputBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateSystemPropertyBuildItem;

public class SubstrateSystemPropertiesBuildStep {

    @BuildStep
    SubstrateOutputBuildItem writeNativeProps(ArchiveRootBuildItem root,
            List<SubstrateSystemPropertyBuildItem> props) throws Exception {

        final Properties properties = new Properties();
        for (SubstrateSystemPropertyBuildItem i : props) {
            if (!properties.containsKey(i.getKey())) {
                properties.put(i.getKey(), i.getValue());
            } else if (!properties.get(i.getKey()).equals(i.getValue())) {
                throw new RuntimeException("Duplicate native image system property under " + i.getKey()
                        + " conflicting values of " + i.getValue() + " and " + properties.get(i.getKey()));
            }
        }
        try (FileOutputStream os = new FileOutputStream(new File(root.getArchiveRoot().toFile(), "native-image.properties"))) {
            try (OutputStreamWriter osw = new OutputStreamWriter(os, StandardCharsets.UTF_8)) {
                properties.store(osw, "Generated properties (do not edit)");
            }
        }
        return new SubstrateOutputBuildItem();
    }
}
