package io.quarkus.registry.catalog.json;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.catalog.ExtensionOrigin;
import java.util.Objects;

@Deprecated
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class JsonExtensionOrigin implements ExtensionOrigin.Mutable {

    protected String id;
    protected boolean platform;
    protected ArtifactCoords bom;

    @Override
    public String getId() {
        return id;
    }

    public Mutable setId(String id) {
        this.id = id;
        return this;
    }

    @Override
    public ArtifactCoords getBom() {
        return bom;
    }

    public Mutable setBom(ArtifactCoords bom) {
        this.bom = bom;
        return this;
    }

    @Override
    public boolean isPlatform() {
        return platform;
    }

    public Mutable setPlatform(boolean platform) {
        this.platform = platform;
        return this;
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

    @Override
    public ExtensionOrigin build() {
        return this;
    }

    @Override
    public ExtensionOrigin.Mutable mutable() {
        return this;
    }
}
