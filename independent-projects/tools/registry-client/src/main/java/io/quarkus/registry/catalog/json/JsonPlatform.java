package io.quarkus.registry.catalog.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.catalog.Platform;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonPlatform implements Platform {

    public static JsonPlatform of(ArtifactCoords bom) {
        JsonPlatform p = new JsonPlatform();
        p.setBom(Objects.requireNonNull(bom, "bom can't be null"));
        return p;
    }

    private ArtifactCoords bom;
    private String quarkusCore;
    private String upstreamQuarkusCore;

    @Override
    public ArtifactCoords getBom() {
        return bom;
    }

    public void setBom(ArtifactCoords bom) {
        this.bom = bom;
    }

    @Override
    public String getQuarkusCoreVersion() {
        return quarkusCore;
    }

    public void setQuarkusCoreVersion(String quarkusCore) {
        this.quarkusCore = quarkusCore;
    }

    @Override
    public String getUpstreamQuarkusCoreVersion() {
        return upstreamQuarkusCore;
    }

    public void setUpstreamQuarkusCoreVersion(String upstreamQuarkusCore) {
        this.upstreamQuarkusCore = upstreamQuarkusCore;
    }

    @Override
    public String toString() {
        return "[platform " + bom + ", quarkus-core=" + quarkusCore + ", upstream-quarkus-core=" + upstreamQuarkusCore + "]";
    }
}
