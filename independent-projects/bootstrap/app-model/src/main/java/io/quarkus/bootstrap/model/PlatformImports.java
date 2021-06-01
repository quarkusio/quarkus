package io.quarkus.bootstrap.model;

import java.util.Map;

public interface PlatformImports {

    public Map<String, String> getPlatformProperties();

    public String getMisalignmentReport();

    public boolean isAligned();
}
