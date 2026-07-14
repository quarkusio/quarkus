package io.quarkus.hibernate.orm.runtime.boot.scan;

import java.util.Set;

public interface ScanResult {
        /**
         * Returns descriptors for all packages discovered as part of the scan
         *
         * @return Descriptors for discovered packages
         */
        Set<QuarkusScanner.PackageDescriptorImpl> getLocatedPackages();

        /**
         * Returns descriptors for all classes discovered as part of the scan
         *
         * @return Descriptors for discovered classes
         */
        Set<QuarkusScanner.ClassDescriptorImpl> getLocatedClasses();

    }