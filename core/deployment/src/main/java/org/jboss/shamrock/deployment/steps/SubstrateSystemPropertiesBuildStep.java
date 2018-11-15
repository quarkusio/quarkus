package org.jboss.shamrock.deployment.steps;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;

import org.jboss.shamrock.annotations.BuildStep;
import org.jboss.shamrock.deployment.builditem.ArchiveRootBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.SubstrateOutputBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.SubstrateSystemPropertyBuildItem;

public class SubstrateSystemPropertiesBuildStep {

    @BuildStep
    SubstrateOutputBuildItem writeNativeProps(ArchiveRootBuildItem root,
                                              List<SubstrateSystemPropertyBuildItem> props) throws Exception {

        final Properties properties = new Properties();
        for (SubstrateSystemPropertyBuildItem i : props) {
            if (properties.containsKey(i.getKey())) {
                throw new RuntimeException("Duplicate native image system property under " + i.getKey() + " conflicting values of " + i.getValue() + " and " + properties.get(i.getKey()));
            }
            properties.put(i.getKey(), i.getValue());
        }
        try (FileOutputStream os = new FileOutputStream(new File(root.getPath().toFile(), "native-image.properties"))) {
            try (OutputStreamWriter osw = new OutputStreamWriter(os, StandardCharsets.UTF_8)) {
                properties.store(osw, "Generated properties (do not edit)");
            }
        }
        return new SubstrateOutputBuildItem();
    }
}
