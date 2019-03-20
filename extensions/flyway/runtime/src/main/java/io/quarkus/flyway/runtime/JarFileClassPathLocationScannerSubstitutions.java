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
import java.util.Set;

import org.flywaydb.core.internal.scanner.classpath.ClassPathLocationScanner;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * This substitution is neccessary because
 * {@link org.flywaydb.core.internal.scanner.classpath.JarFileClassPathLocationScanner#JarFileClassPathLocationScanner(String)}
 * constructor is package private
 * and cannot be accessed from {@link ClassPathScannerSubstitutions#createLocationScanner(String)}
 */
@TargetClass(className = "org.flywaydb.core.internal.scanner.classpath.JarFileClassPathLocationScanner")
public final class JarFileClassPathLocationScannerSubstitutions implements ClassPathLocationScanner {

    @Alias
    public JarFileClassPathLocationScannerSubstitutions(String separator) {
        // SVM will call the original method because of the @Alias annotationø
    }

    @Alias
    @Override
    public Set<String> findResourceNames(String location, URL locationUrl) {
        // SVM will call the original method because of the @Alias annotation
        return null;
    }
}
