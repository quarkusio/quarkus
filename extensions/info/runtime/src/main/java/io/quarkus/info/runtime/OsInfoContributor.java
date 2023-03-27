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
    public Map<String, Object> data() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("name", System.getProperty("os.name"));
        result.put("version", System.getProperty("os.version"));
        result.put("arch", System.getProperty("os.arch"));
        return result;
    }
}
