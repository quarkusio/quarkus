package io.quarkus.info.runtime;

import java.util.LinkedHashMap;
import java.util.Map;

import io.quarkus.info.runtime.spi.InfoContributor;

public class OsInfoContributor implements InfoContributor {
    @Override
    public String name() {
        return "os";
    }

    @Override
    public String displayName() {
        return "Operating System";
    }

    @Override
    public Map<String, Object> data() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("name", getName());
        result.put("version", getVersion());
        result.put("arch", getArchitecture());
        return result;
    }

    static String getName() {
        return System.getProperty("os.name");
    }

    static String getVersion() {
        return System.getProperty("os.version");
    }

    static String getArchitecture() {
        return System.getProperty("os.arch");
    }
}
