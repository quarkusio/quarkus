/*
 * Copyright 2019 Red Hat, Inc.
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
package io.quarkus.test.common;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class PropertyTestUtil {

    public static void setLogFileProperty() {
        System.setProperty("quarkus.log.file.path", getLogFileLocation());
    }

    public static String getLogFileLocation() {
        if (Files.isDirectory(Paths.get("build"))) {
            return "build" + File.separator + "quarkus.log";
        }
        return "target" + File.separator + "quarkus.log";
    }
}
