package io.quarkus.flyway.runtime;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.flywaydb.core.api.Location;
import org.flywaydb.core.internal.resource.LoadableResource;
import org.flywaydb.core.internal.scanner.classpath.ClassPathScanner;
import org.flywaydb.core.internal.scanner.classpath.ResourceAndClassScanner;
import org.flywaydb.core.internal.scanner.filesystem.FileSystemScanner;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * @author cristhiank on 2019-03-19
 **/
@TargetClass(className = "org.flywaydb.core.internal.scanner.Scanner")
public final class ScannerSubstitutions {

    @Alias
    private List<LoadableResource> resources = new ArrayList<>();
    @Alias
    private List<Class<?>> classes = new ArrayList<>();

    @Substitute
    public ScannerSubstitutions(Collection<Location> locations, ClassLoader classLoader, Charset encoding) {
        FileSystemScanner fileSystemScanner = new FileSystemScanner(encoding);
        for (Location location : locations) {
            if (location.isFileSystem()) {
                resources.addAll(fileSystemScanner.scanForResources(location));
            } else {
                ResourceAndClassScanner resourceAndClassScanner = new ClassPathScanner(
                        classLoader,
                        encoding,
                        location);
                resources.addAll(resourceAndClassScanner.scanForResources());
                classes.addAll(resourceAndClassScanner.scanForClasses());
            }
        }
    }
}
