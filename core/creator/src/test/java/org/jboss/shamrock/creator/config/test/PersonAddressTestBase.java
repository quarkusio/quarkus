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

package org.jboss.shamrock.creator.config.test;

import static org.junit.Assert.assertEquals;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.jboss.shamrock.creator.config.reader.PropertiesConfigReader;
import org.jboss.shamrock.creator.config.reader.PropertiesHandler;
import org.jboss.shamrock.creator.config.reader.PropertyLine;
import org.jboss.shamrock.creator.util.IoUtils;
import org.junit.Test;

/**
 *
 * @author Alexey Loubyansky
 */
public abstract class PersonAddressTestBase {

    private static final String FIRST_NAME = "First";
    private static final String LAST_NAME = "Last";

    private static final String HOME_STREET = "Home Street";
    private static final String HOME_ZIP = "123";
    private static final String HOME_CITY = "Homecity";

    private static final String WORK_STREET = "Work Street";
    private static final String WORK_ZIP = "456";
    private static final String WORK_CITY = "Workcity";

    @Test
    public void runExample() throws Exception {

        /*
         * Init example properties
         */
        final Properties props = new Properties();
        props.setProperty("first-name", FIRST_NAME);
        props.setProperty("last-name", LAST_NAME);
        props.setProperty("home-address.street", HOME_STREET);
        props.setProperty("home-address.zip", HOME_ZIP);
        props.setProperty("home-address.city", HOME_CITY);
        props.setProperty("work-address.street", WORK_STREET);
        props.setProperty("work-address.zip", WORK_ZIP);
        props.setProperty("work-address.city", WORK_CITY);
        // properties that don't belong to a person
        props.setProperty("other.prop1", "v1");
        props.setProperty("other.prop2", "v2");

        final Path tmpDir = IoUtils.createRandomTmpDir();
        final Person loaded;
        final Map<String, String> loadedNotMapped = new HashMap<>();
        try {
            /*
             * Store properties in a file
             */
            final Path propsFile = tmpDir.resolve("example.properties");
            try(OutputStream out = Files.newOutputStream(propsFile)) {
                props.store(out, "");
            }

            /*
             * Parse the properties file
             */
            loaded = PropertiesConfigReader.getInstance(
                    getPropertiesHandler(), // properties handler
                    (PropertyLine line) -> loadedNotMapped.put(line.getName(), line.getValue()) // what to do with the props not recognized by the handler
                    )
                    .read(propsFile);
        } finally {
            IoUtils.recursiveDelete(tmpDir);
        }

        /*
         * Make sure the result is correct
         */
        final Person expected = new Person();
        expected.setFirstName(FIRST_NAME);
        expected.setLastName(LAST_NAME);
        expected.setHomeAddress(Address.builder().setCity(HOME_CITY).setZip(HOME_ZIP).setStreet(HOME_STREET).build());
        expected.setWorkAddress(Address.builder().setCity(WORK_CITY).setZip(WORK_ZIP).setStreet(WORK_STREET).build());
        assertEquals(expected, loaded);

        final Map<String, String> expectedNotMapped = new HashMap<>(2);
        expectedNotMapped.put("other.prop1", "v1");
        expectedNotMapped.put("other.prop2", "v2");
        assertEquals(expectedNotMapped, loadedNotMapped);
    }

    protected abstract PropertiesHandler<Person> getPropertiesHandler();
}
