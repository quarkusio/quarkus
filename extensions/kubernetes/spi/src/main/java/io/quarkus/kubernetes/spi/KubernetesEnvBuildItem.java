
package io.quarkus.kubernetes.spi;

import io.quarkus.builder.item.MultiBuildItem;

public final class KubernetesEnvBuildItem extends MultiBuildItem {

    public enum EnvType {
        var(false),
        field(false),
        secret(true),
        configmap(true);

        public final boolean allowMultipleDefinitions;

        EnvType(boolean allowMultipleDefinitions) {
            this.allowMultipleDefinitions = allowMultipleDefinitions;
        }

        public boolean mightConflictWith(EnvType type) {
            if (this == type) {
                return true;
            }

            switch (this) {
                case field:
                    return type == var;
                case var:
                    return type == field;
                case secret:
                    return type == configmap;
                case configmap:
                    return type == secret;
                default:
                    return false;
            }
        }
    }

    private final String name;
    private final String value;
    private final EnvType type;
    private final String target;
    private final boolean oldStyle;

    public static EnvType getEnvType(String secret, String configmap, String field) {
        final EnvType type;
        if (secret != null) {
            type = EnvType.secret;
        } else if (configmap != null) {
            type = EnvType.configmap;
        } else if (field != null) {
            type = EnvType.field;
        } else {
            type = EnvType.var;
        }
        return type;
    }

    public KubernetesEnvBuildItem(String name, String value, String target) {
        this(EnvType.var, name, value, target);
    }

    public KubernetesEnvBuildItem(EnvType type, String name, String value, String target, boolean oldStyle) {
        this.name = name;
        this.value = value;
        this.type = type;
        this.target = target;
        this.oldStyle = oldStyle;
    }

    public KubernetesEnvBuildItem(EnvType type, String name, String value, String target) {
        this(type, name, value, target, false);
    }

    public String getConfigMap() {
        return getValueIfMatching(EnvType.configmap);
    }

    public String getSecret() {
        return getValueIfMatching(EnvType.secret);
    }

    public String getField() {
        return getValueIfMatching(EnvType.field);
    }

    public String getVar() {
        return getValueIfMatching(EnvType.var);
    }

    public boolean isOldStyle() {
        return oldStyle;
    }

    private String getValueIfMatching(EnvType type) {
        return this.type == type ? value : null;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public EnvType getType() {
        return type;
    }

    public String getTarget() {
        return target;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        KubernetesEnvBuildItem that = (KubernetesEnvBuildItem) o;

        if (!name.equals(that.name))
            return false;
        if (!value.equals(that.value))
            return false;
        if (type != that.type)
            return false;
        return target.equals(that.target);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + value.hashCode();
        result = 31 * result + type.hashCode();
        result = 31 * result + target.hashCode();
        return result;
    }

}
