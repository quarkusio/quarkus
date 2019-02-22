/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
            if (properties.containsKey(i.getKey())) {
                throw new RuntimeException("Duplicate native image system property under " + i.getKey()
                        + " conflicting values of " + i.getValue() + " and " + properties.get(i.getKey()));
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
