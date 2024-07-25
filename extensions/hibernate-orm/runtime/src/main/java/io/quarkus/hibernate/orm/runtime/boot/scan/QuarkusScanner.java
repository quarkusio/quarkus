package io.quarkus.hibernate.orm.runtime.boot.scan;

import static io.quarkus.commons.classloading.ClassLoaderHelper.fromClassNameToResourceName;

import java.util.Collections;
import java.util.HashSet;
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

    private Set<PackageDescriptor> packageDescriptors;
    private Set<ClassDescriptor> classDescriptors;

    @Override
    public ScanResult scan(ScanEnvironment scanEnvironment, ScanOptions scanOptions, ScanParameters scanParameters) {
        return new Result(packageDescriptors, classDescriptors, scanEnvironment, scanOptions);
    }

    public Set<PackageDescriptor> getPackageDescriptors() {
        return packageDescriptors;
    }

    public void setPackageDescriptors(Set<PackageDescriptor> packageDescriptors) {
        this.packageDescriptors = packageDescriptors;
    }

    public Set<ClassDescriptor> getClassDescriptors() {
        return classDescriptors;
    }

    public void setClassDescriptors(Set<ClassDescriptor> classDescriptors) {
        this.classDescriptors = classDescriptors;
    }

    public static class Result implements ScanResult {

        private final Set<PackageDescriptor> selectedPackageDescriptors;
        private final Set<ClassDescriptor> selectedClassDescriptors;

        Result(Set<PackageDescriptor> packageDescriptors, Set<ClassDescriptor> classDescriptors,
                ScanEnvironment scanEnvironment, ScanOptions scanOptions) {
            this.selectedPackageDescriptors = new HashSet<>();
            this.selectedClassDescriptors = new HashSet<>();

            for (PackageDescriptor packageDescriptor : packageDescriptors) {
                if (scanOptions.canDetectUnlistedClassesInRoot() ||
                        scanEnvironment.getExplicitlyListedClassNames().contains(packageDescriptor.getName())) {
                    this.selectedPackageDescriptors.add(packageDescriptor);
                }
            }

            for (ClassDescriptor classDescriptor : classDescriptors) {
                if (scanOptions.canDetectUnlistedClassesInRoot() ||
                        scanEnvironment.getExplicitlyListedClassNames().contains(classDescriptor.getName())) {
                    this.selectedClassDescriptors.add(classDescriptor);
                }
            }
        }

        @Override
        public Set<PackageDescriptor> getLocatedPackages() {
            return selectedPackageDescriptors;
        }

        @Override
        public Set<ClassDescriptor> getLocatedClasses() {
            return selectedClassDescriptors;
        }

        @Override
        public Set<MappingFileDescriptor> getLocatedMappingFiles() {
            //TODO: handle hbm files
            return Collections.emptySet();
        }
    }

    public static class PackageDescriptorImpl implements PackageDescriptor {

        private String name;

        public PackageDescriptorImpl(String name) {
            this.name = name;
        }

        public PackageDescriptorImpl() {
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public InputStreamAccess getStreamAccess() {
            return new UrlInputStreamAccess(
                    Thread.currentThread().getContextClassLoader().getResource(name.replace('.', '/') + "/package-info.class"));
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
            final String resourceName = fromClassNameToResourceName(name);
            return new UrlInputStreamAccess(
                    Thread.currentThread().getContextClassLoader().getResource(resourceName));
        }
    }
}
