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

package io.quarkus.flyway.runtime;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.flywaydb.core.internal.scanner.classpath.ClassPathLocationScanner;
import org.flywaydb.core.internal.scanner.classpath.FileSystemClassPathLocationScanner;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "org.flywaydb.core.internal.scanner.classpath.ClassPathScanner")
public final class ClassPathScannerSubstitutions {
    @Alias
    private Map<String, ClassPathLocationScanner> locationScannerCache = new HashMap<>();
    @Alias
    private Map<ClassPathLocationScanner, Map<URL, Set<String>>> resourceNameCache = new HashMap<>();

    /**
     * Substitution to remove location scanners that are not supported.
     * It removes jboss-vfs and OSGi functionality from Flyway to avoid native compilation errors
     * because of incomplete classpath as these dependencies are optional in Flyway
     * 
     * @see org.flywaydb.core.internal.scanner.classpath.ClassPathScanner#createLocationScanner(String)
     * 
     */
    @Substitute
    private ClassPathLocationScanner createLocationScanner(String protocol) {
        if (locationScannerCache.containsKey(protocol)) {
            return locationScannerCache.get(protocol);
        }

        if ("file".equals(protocol)) {
            FileSystemClassPathLocationScanner locationScanner = new FileSystemClassPathLocationScanner();
            locationScannerCache.put(protocol, locationScanner);
            resourceNameCache.put(locationScanner, new HashMap<>());
            return locationScanner;
        }

        if ("jar".equals(protocol)) {
            String separator = "!/";
            ClassPathLocationScanner locationScanner = new JarFileClassPathLocationScannerSubstitutions(separator);
            locationScannerCache.put(protocol, locationScanner);
            resourceNameCache.put(locationScanner, new HashMap<>());
            return locationScanner;
        }
        return null;
    }
}
