/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.shamrock.creator.demo;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.jboss.shamrock.creator.AppCreator;
import org.jboss.shamrock.creator.config.AppCreatorPropertiesHandler;
import org.jboss.shamrock.creator.config.reader.PropertiesConfigReader;
import org.jboss.shamrock.creator.config.reader.PropertiesConfigReaderException;
import org.jboss.shamrock.creator.config.reader.PropertyLine;
import org.jboss.shamrock.creator.config.reader.UnrecognizedPropertyHandler;
import org.jboss.shamrock.creator.util.IoUtils;
import org.jboss.shamrock.creator.util.PropertyUtils;

/**
 *
 * @author Alexey Loubyansky
 */
public class AppCreatorPropertiesConfigDemo {

    public static void main(String[] args) throws Exception {

        final Path shamrockRoot = Paths.get("").toAbsolutePath().getParent();
        final Path exampleTarget = shamrockRoot.resolve("examples").resolve("bean-validation-strict").resolve("target");

        final Path appJar = exampleTarget.resolve("shamrock-strict-bean-validation-example-1.0.0.Alpha1-SNAPSHOT.jar");
        if (!Files.exists(appJar)) {
            throw new Exception("Failed to locate user app " + appJar);
        }

        final Path demoDir = Paths.get(PropertyUtils.getUserHome()).resolve("shamrock-creator-demo");
        IoUtils.recursiveDelete(demoDir);

        final Properties props = new Properties();
        props.setProperty("augment", "true");
        props.setProperty("native-image.output", demoDir.toString());
        props.setProperty("native-image.disable-reports", "true");

        Files.createDirectories(demoDir);
        final Path propsFile = demoDir.resolve("app-creator.properties");
        try(OutputStream out = Files.newOutputStream(propsFile)) {
            props.store(out, "Example AppCreator properties");
        }

        Map<String, String> notMappedProps = new HashMap<>(0);
        final AppCreator appCreator = PropertiesConfigReader
                .getInstance(new AppCreatorPropertiesHandler(), new UnrecognizedPropertyHandler() {
                    @Override
                    public void unrecognizedProperty(PropertyLine line) throws PropertiesConfigReaderException {
                        // System.out.println("Unrecognized " + line);
                        notMappedProps.put(line.getName(), line.getValue());
                    }
                }).read(propsFile);
        if(!notMappedProps.isEmpty()) {
            final List<String> names = new ArrayList<>(notMappedProps.keySet());
            Collections.sort(names);
            System.out.println("Not mapped:");
            for(String name : names) {
                System.out.println("- " + name + "=" + notMappedProps.get(name));
            }
        }
        appCreator.create(appJar);
    }
}
