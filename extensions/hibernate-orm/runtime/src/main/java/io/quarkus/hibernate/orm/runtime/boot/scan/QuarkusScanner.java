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

package io.quarkus.hibernate.orm.runtime.boot.scan;

import java.util.Collections;
import java.util.Set;

import org.hibernate.boot.archive.internal.UrlInputStreamAccess;
import org.hibernate.boot.archive.scan.spi.ClassDescriptor;
import org.hibernate.boot.archive.scan.spi.MappingFileDescriptor;
import org.hibernate.boot.archive.scan.spi.PackageDescriptor;
import org.hibernate.boot.archive.scan.spi.ScanEnvironment;
import org.hibernate.boot.archive.scan.spi.ScanOptions;
import org.hibernate.boot.archive.scan.spi.ScanParameters;
import org.hibernate.boot.archive.scan.spi.ScanResult;
import org.hibernate.boot.archive.scan.spi.Scanner;
import org.hibernate.boot.archive.spi.InputStreamAccess;

/**
 * A hard coded scanner. This scanner is serialized to bytecode, and used to avoid scanning on Hibernate startup.
 * Technically the scanners are receiving all classes and categorize them as JPA useful or not.
 * In Quarkus's case, we detect the JPA friendly ones and not list the other ones.
 * Emmanuel thinks it's fine as AFAICS, Hibernate ORM filter out the non JPA specific ones.
 */
public class QuarkusScanner implements Scanner {

    private Set<ClassDescriptor> classDescriptors;

    @Override
    public ScanResult scan(ScanEnvironment scanEnvironment, ScanOptions scanOptions, ScanParameters scanParameters) {
        return new Result();
    }

    public Set<ClassDescriptor> getClassDescriptors() {
        return classDescriptors;
    }

    public void setClassDescriptors(Set<ClassDescriptor> classDescriptors) {
        this.classDescriptors = classDescriptors;
    }

    public class Result implements ScanResult {

        @Override
        public Set<PackageDescriptor> getLocatedPackages() {
            //todo: handle packages
            return Collections.emptySet();
        }

        @Override
        public Set<ClassDescriptor> getLocatedClasses() {
            return classDescriptors;
        }

        @Override
        public Set<MappingFileDescriptor> getLocatedMappingFiles() {
            //TODO: handle hbm files
            return Collections.emptySet();
        }
    }

    public static class ClassDescriptorImpl implements ClassDescriptor {

        private String name;
        private Categorization categorization;

        public ClassDescriptorImpl(String name, Categorization categorization) {
            this.name = name;
            this.categorization = categorization;
        }

        public ClassDescriptorImpl() {
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setCategorization(Categorization categorization) {
            this.categorization = categorization;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Categorization getCategorization() {
            return categorization;
        }

        @Override
        public InputStreamAccess getStreamAccess() {
            return new UrlInputStreamAccess(
                    Thread.currentThread().getContextClassLoader().getResource(name.replace('.', '/') + ".class"));
        }
    }
}
