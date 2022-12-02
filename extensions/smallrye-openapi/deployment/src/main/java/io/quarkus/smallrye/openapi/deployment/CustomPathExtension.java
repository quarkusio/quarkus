package io.quarkus.smallrye.openapi.deployment;

import java.util.Collection;

import org.jboss.jandex.ClassInfo;

import io.smallrye.openapi.runtime.scanner.AnnotationScannerExtension;
import io.smallrye.openapi.runtime.scanner.spi.AnnotationScanner;

/**
 * This adds support for the quarkus.http.root-path config option
 */
public class CustomPathExtension implements AnnotationScannerExtension {

    private final String defaultPath;

    public CustomPathExtension(String defaultPath) {
        this.defaultPath = defaultPath;
    }

    @Override
    public void processScannerApplications(AnnotationScanner scanner, Collection<ClassInfo> applications) {
        scanner.setContextRoot(defaultPath);
    }
}