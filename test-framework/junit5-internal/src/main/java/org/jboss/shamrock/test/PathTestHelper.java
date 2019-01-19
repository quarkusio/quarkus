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

package org.jboss.shamrock.test;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

final class PathTestHelper {

    private static final String TEST_CLASSES_FRAGMENT = "/test-classes";
    private static final String CLASSES_FRAGMENT = "/classes";

    private PathTestHelper() {
    }

    public static final Path getTestClassesLocation(Class<?> testClass) {
        String classFileName = testClass.getName().replace('.', '/') + ".class";
        URL resource = testClass.getClassLoader().getResource(classFileName);

        String path = resource.getPath();

        if (!path.contains(TEST_CLASSES_FRAGMENT)) {
            throw new RuntimeException("The test class " + testClass + " is not located in the " + TEST_CLASSES_FRAGMENT + " directory.");
        }

        return Paths.get(resource.getPath().substring(0, resource.getPath().length() - classFileName.length()));
    }

    public static final Path getAppClassLocation(Class<?> testClass) {
        return Paths.get(getTestClassesLocation(testClass).toString().replace(TEST_CLASSES_FRAGMENT, CLASSES_FRAGMENT));
    }
}
