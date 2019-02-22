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

package org.jboss.builder;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class Version {
    private static final String VERSION;
    private static final String JAR_NAME;

    static {
        Properties versionProps = new Properties();
        String versionString = "(unknown)";
        String jarName = "(unknown)";
        try (final InputStream stream = Version.class.getResourceAsStream("Version.properties")) {
            if (stream != null)
                try (final InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    versionProps.load(reader);
                    versionString = versionProps.getProperty("version", versionString);
                    jarName = versionProps.getProperty("jarName", jarName);
                }
        } catch (IOException ignored) {
        }
        VERSION = versionString;
        JAR_NAME = jarName;
    }

    /**
     * Get the version.
     *
     * @return the version
     */
    public static String getVersion() {
        return VERSION;
    }

    /**
     * Get the JAR name.
     *
     * @return the JAR name
     */
    public static String getJarName() {
        return JAR_NAME;
    }

    /**
     * Print out the current version on {@code System.out}.
     *
     * @param args ignored
     */
    public static void main(String[] args) {
        System.out.printf("WildFly Deployer (%s) version %s%n", JAR_NAME, VERSION);
    }
}
