package io.quarkus.platform.templates;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ProjectConfigTemplate {

    public static class Builder {

        private Map<String, PropertyTemplate> props = Collections.emptyMap();
        private BomTemplate bom;

        private Builder() {
        }

        public Builder addProperty(PropertyTemplate prop) {
            if(props.isEmpty()) {
                props = new HashMap<>();
            }
            props.put(prop.getName(), prop);
            return this;
        }

        public Builder setPlatformBom(BomTemplate bom) {
            this.bom = bom;
            return this;
        }

        public ProjectConfigTemplate build() {
            return new ProjectConfigTemplate(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private final Map<String, PropertyTemplate> props;
    private final BomTemplate bom;

    private ProjectConfigTemplate(Builder builder) {
        this.props = builder.props.isEmpty() ? builder.props : Collections.unmodifiableMap(builder.props);
        this.bom = builder.bom;
    }

    public BomTemplate getPlatformBom() {
        return bom;
    }

    public Map<String, PropertyTemplate> getProperties() {
        return props;
    }
}
