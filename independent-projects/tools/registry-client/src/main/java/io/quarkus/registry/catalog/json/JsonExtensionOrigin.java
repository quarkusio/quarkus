package io.quarkus.registry.catalog.json;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.catalog.ExtensionOrigin;
import java.util.Objects;

@Deprecated
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class JsonExtensionOrigin implements ExtensionOrigin {

    protected String id;
    protected boolean platform;
    protected ArtifactCoords bom;

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public ArtifactCoords getBom() {
        return bom;
    }

    public void setBom(ArtifactCoords bom) {
        this.bom = bom;
    }

    @Override
    public boolean isPlatform() {
        return platform;
    }

    public void setPlatform(boolean platform) {
        this.platform = platform;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        JsonExtensionOrigin that = (JsonExtensionOrigin) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append('[');
        if (id != null) {
            buf.append("id=").append(id).append(' ');
        }
        buf.append("platform=").append(platform);
        buf.append(" boms=").append(bom);
        return buf.append(']').toString();
    }
}
