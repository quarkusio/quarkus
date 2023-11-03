package io.quarkus.liquibase;

import java.io.FileNotFoundException;

import liquibase.resource.ResourceAccessor;
import liquibase.resource.ZipPathHandler;

// https://github.com/liquibase/liquibase/issues/3524#issuecomment-1465282155
public class LiquibaseLenientZipPathHandler extends ZipPathHandler {

    @Override
    public int getPriority(String root) {
        if (root != null && root.startsWith("jar:") && root.contains("!/")) {
            return PRIORITY_SPECIALIZED;
        }
        return PRIORITY_NOT_APPLICABLE;
    }

    @Override
    public ResourceAccessor getResourceAccessor(String root) throws FileNotFoundException {
        int idx = root.indexOf("!/");
        return super.getResourceAccessor(idx > 0 ? root.substring(0, idx) : root);
    }
}