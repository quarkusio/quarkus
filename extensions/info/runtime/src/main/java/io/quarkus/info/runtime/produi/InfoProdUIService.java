package io.quarkus.info.runtime.produi;

import java.util.Collections;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.runtime.annotations.JsonRpcDescription;
import io.quarkus.runtime.annotations.JsonRpcUsage;
import io.quarkus.runtime.annotations.Usage;
import io.smallrye.common.annotation.NonBlocking;

/**
 * Read-only Prod UI view of the application and environment info (OS, Java,
 * build and git). It serves the exact same aggregated map that backs the
 * {@code /q/info} endpoint - populated at runtime init by {@code InfoRecorder} -
 * so the existing {@code qwc-info.js} Dev UI component can be reused unchanged.
 * It exposes only build-time metadata (no credentials); any sensitive remote URL
 * is already sanitized upstream by the info extension.
 */
@ApplicationScoped
public class InfoProdUIService {

    private volatile Map<String, Object> info = Collections.emptyMap();

    public void setInfo(Map<String, Object> info) {
        this.info = info;
    }

    @NonBlocking
    @JsonRpcUsage(Usage.PROD_UI)
    @JsonRpcDescription("Get a read-only view of the application and environment info (OS, Java, build and git)")
    public Map<String, Object> getApplicationAndEnvironmentInfo() {
        return info;
    }
}
