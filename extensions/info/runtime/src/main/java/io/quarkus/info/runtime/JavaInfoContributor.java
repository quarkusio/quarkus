package io.quarkus.info.runtime;

import java.util.LinkedHashMap;
import java.util.Map;

import io.quarkus.info.runtime.spi.InfoContributor;

public class JavaInfoContributor implements InfoContributor {

    @Override
    public String name() {
        return "java";
    }

    @Override
    public String displayName() {
        return "Java";
    }

    @Override
    public Map<String, Object> data() {
        //TODO: should we add more information like 'java.runtime.*' and 'java.vm.*' ?
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("version", getVersion());
        result.put("vendor", getVendor());
        result.put("vendorVersion", getVendorVersion());
        return result;
    }

    static String getVersion() {
        return System.getProperty("java.version");
    }

    static String getVendor() {
        return System.getProperty("java.vendor");
    }

    static String getVendorVersion() {
        return System.getProperty("java.vendor.version");
    }
}
