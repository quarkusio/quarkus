package io.quarkus.hibernate.orm.runtime.boot.scan;

import static io.quarkus.commons.classloading.ClassLoaderHelper.fromClassNameToResourceName;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.boot.archive.spi.InputStreamAccess;

/**
 * A hard coded scanner. This scanner is serialized to bytecode, and used to avoid scanning on Hibernate startup.
 * Technically the scanners are receiving all classes and categorize them as JPA useful or not.
 * In Quarkus's case, we detect the JPA friendly ones and not list the other ones.
 * Emmanuel thinks it's fine as AFAICS, Hibernate ORM filter out the non JPA specific ones.
 */
public class QuarkusScanner implements io.quarkus.hibernate.orm.runtime.boot.scan.Scanner {

    private Set<PackageDescriptorImpl> packageDescriptors;
    private Set<ClassDescriptorImpl> classDescriptors;

    public Result scan() {
        return new Result(packageDescriptors, classDescriptors);
    }

    public Set<PackageDescriptorImpl> getPackageDescriptors() {
        return packageDescriptors;
    }

    public void setPackageDescriptors(Set<PackageDescriptorImpl> packageDescriptors) {
        this.packageDescriptors = packageDescriptors;
    }

    public Set<ClassDescriptorImpl> getClassDescriptors() {
        return classDescriptors;
    }

    public void setClassDescriptors(Set<ClassDescriptorImpl> classDescriptors) {
        this.classDescriptors = classDescriptors;
    }

    public static class Result implements ScanResult {

        private final Set<PackageDescriptorImpl> selectedPackageDescriptors;
        private final Set<ClassDescriptorImpl> selectedClassDescriptors;

        Result(Set<PackageDescriptorImpl> packageDescriptors, Set<ClassDescriptorImpl> classDescriptors) {
            this.selectedPackageDescriptors = new HashSet<>();
            this.selectedClassDescriptors = new HashSet<>();

            for (PackageDescriptorImpl packageDescriptor : packageDescriptors) {
                // TODO Luca figure out this if if it's tested
                //                if (scanOptions.canDetectUnlistedClassesInRoot() ||
                //                        scanEnvironment.getExplicitlyListedClassNames().contains(packageDescriptor.getName())) {
                this.selectedPackageDescriptors.add(packageDescriptor);
                //                }
            }

            for (ClassDescriptorImpl classDescriptor : classDescriptors) {
                //                if (scanOptions.canDetectUnlistedClassesInRoot() ||
                //                        scanEnvironment.getExplicitlyListedClassNames().contains(classDescriptor.getName())) {
                this.selectedClassDescriptors.add(classDescriptor);
            }
            //            }
        }

        @Override
        public Set<PackageDescriptorImpl> getLocatedPackages() {
            return selectedPackageDescriptors;
        }

        @Override
        public Set<ClassDescriptorImpl> getLocatedClasses() {
            return selectedClassDescriptors;
        }
    }

    public static class PackageDescriptorImpl {

        private String name;

        public PackageDescriptorImpl(String name) {
            this.name = name;
        }

        public PackageDescriptorImpl() {
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public InputStreamAccess getStreamAccess() {
            return new UrlInputStreamAccess(
                    Thread.currentThread().getContextClassLoader().getResource(name.replace('.', '/') + "/package-info.class"));
        }
    }

    public static class ClassDescriptorImpl {

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

        public String getName() {
            return name;
        }

        public Categorization getCategorization() {
            return categorization;
        }

        public InputStreamAccess getStreamAccess() {
            final String resourceName = fromClassNameToResourceName(name);
            return new UrlInputStreamAccess(
                    Thread.currentThread().getContextClassLoader().getResource(resourceName));
        }
    }
}
