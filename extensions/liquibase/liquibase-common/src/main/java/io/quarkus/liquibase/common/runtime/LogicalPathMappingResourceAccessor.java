package io.quarkus.liquibase.common.runtime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import liquibase.changelog.DatabaseChangeLog;
import liquibase.resource.ResourceAccessor;

/**
 * Delegates to another {@link ResourceAccessor}, remapping Liquibase {@code logicalFilePath} keys to
 * physical classpath resource paths when present in the mapping table (native image only).
 */
public final class LogicalPathMappingResourceAccessor implements ResourceAccessor {

    private final Map<String, String> logicalToPhysical;
    private final ResourceAccessor delegate;

    public LogicalPathMappingResourceAccessor(Map<String, String> logicalToPhysical, ResourceAccessor delegate) {
        this.logicalToPhysical = logicalToPhysical == null ? Map.of() : logicalToPhysical;
        this.delegate = delegate;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List search(String path, boolean recursive) throws IOException {
        return delegate.search(remap(path), recursive);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List getAll(String path) throws IOException {
        return delegate.getAll(remap(path));
    }

    private String remap(String path) {
        if (path == null || logicalToPhysical.isEmpty()) {
            return path;
        }
        String normalized = DatabaseChangeLog.normalizePath(path);
        String physical = logicalToPhysical.get(normalized);
        return physical != null ? physical : path;
    }

    @Override
    public List<String> describeLocations() {
        if (logicalToPhysical.isEmpty()) {
            return delegate.describeLocations();
        }
        List<String> locations = new ArrayList<>(1 + delegate.describeLocations().size());
        locations.add("Liquibase logicalFilePath mappings (" + logicalToPhysical.size() + " entries)");
        locations.addAll(delegate.describeLocations());
        return Collections.unmodifiableList(locations);
    }

    @Override
    public void close() throws Exception {
        delegate.close();
    }
}
