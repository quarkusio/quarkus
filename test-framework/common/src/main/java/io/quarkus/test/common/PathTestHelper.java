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
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class PathTestHelper {

    private static final String TEST_CLASSES_FRAGMENT_MAVEN = File.separator + "test-classes";
    private static final String CLASSES_FRAGMENT_MAVEN = File.separator + "classes";
    private static final String TEST_CLASSES_FRAGMENT_GRADLE = "classes" + File.separator + "java" + File.separator + "test";
    private static final String CLASSES_FRAGMENT_GRADLE = "classes" + File.separator + "java" + File.separator + "main";

    private PathTestHelper() {
    }

    public static Path getTestClassesLocation(Class<?> testClass) {
        String classFileName = testClass.getName().replace('.', File.separatorChar) + ".class";
        URL resource = testClass.getClassLoader().getResource(classFileName);

        try {
            Path path = Paths.get(resource.toURI());
            if (path.toString().contains(TEST_CLASSES_FRAGMENT_MAVEN) ||
                    path.toString().contains(TEST_CLASSES_FRAGMENT_GRADLE)) {
                return path.getRoot().resolve(path.subpath(0, path.getNameCount() - Paths.get(classFileName).getNameCount()));
            }
            throw new RuntimeException(
                    "The test class " + testClass + " is not located in the " + TEST_CLASSES_FRAGMENT_MAVEN +
                            " nor in " + TEST_CLASSES_FRAGMENT_GRADLE + " directory.");

        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

    }

    public static Path getAppClassLocation(Class<?> testClass) {
        String testClassPath = getTestClassesLocation(testClass).toString();
        //maven
        if (testClassPath.contains(TEST_CLASSES_FRAGMENT_MAVEN))
            return Paths.get(getTestClassesLocation(testClass).toString()
                    .replace(TEST_CLASSES_FRAGMENT_MAVEN, CLASSES_FRAGMENT_MAVEN));
        //gradle
        else
            return Paths.get(getTestClassesLocation(testClass).toString()
                    .replace(TEST_CLASSES_FRAGMENT_GRADLE, CLASSES_FRAGMENT_GRADLE));
    }

    public static boolean isTestClass(String className, ClassLoader classLoader) {
        String classFileName = className.replace('.', File.separatorChar) + ".class";
        URL resource = classLoader.getResource(classFileName);
        if (!resource.getProtocol().startsWith("file")) {
            return false;
        }
        try {
            Path path = Paths.get(resource.toURI());
            if (path.toString().contains(TEST_CLASSES_FRAGMENT_MAVEN) ||
                    path.toString().contains(TEST_CLASSES_FRAGMENT_GRADLE)) {
                return true;
            } else {
                return false;
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
