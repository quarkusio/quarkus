
package io.quarkus.kubernetes.spi;

import org.jboss.logging.Logger;

import io.quarkus.builder.item.MultiBuildItem;

public final class KubernetesEnvBuildItem extends MultiBuildItem {
    private static final Logger log = Logger.getLogger(KubernetesEnvBuildItem.class);

    public enum EnvType {
        var(false),
        field(false),
        secret(true),
        configmap(true),
        keyFromConfigmap(false),
        keyFromSecret(false);

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
                    return type == var || type == keyFromConfigmap || type == keyFromSecret;
                case var:
                    return type == field || type == keyFromConfigmap || type == keyFromSecret;
                case secret:
                    return type == configmap;
                case configmap:
                    return type == secret;
                case keyFromConfigmap:
                    return type == field || type == var || type == keyFromSecret;
                case keyFromSecret:
                    return type == field || type == var || type == keyFromConfigmap;
                default:
                    return false;
            }
        }
    }

    private final String name;
    private final String value;
    private final String configmap;
    private final String secret;
    private final String field;
    private final EnvType type;
    private final String target;
    private final boolean oldStyle;

    public static KubernetesEnvBuildItem createFromField(String name, String targetField, String target,
            boolean... oldStyle) {
        return create(name, null, null, null, targetField, target, isOldStyle(oldStyle));
    }

    public static KubernetesEnvBuildItem createFromConfigMap(String configMapName, String target, boolean... oldStyle) {
        return create(configMapName, null, null, configMapName, null, target, isOldStyle(oldStyle));
    }

    public static KubernetesEnvBuildItem createFromSecret(String secretName, String target, boolean... oldStyle) {
        return create(secretName, null, secretName, null, null, target, isOldStyle(oldStyle));
    }

    public static KubernetesEnvBuildItem createSimpleVar(String name, String value, String target,
            boolean... oldStyle) {
        return create(name, value, null, null, null, target, isOldStyle(oldStyle));
    }

    public static KubernetesEnvBuildItem createFromConfigMapKey(String varName, String key, String configmap, String target,
            boolean... oldStyle) {
        return create(varName, key, null, configmap, null, target, isOldStyle(oldStyle));
    }

    public static KubernetesEnvBuildItem createFromSecretKey(String varName, String key, String secret, String target,
            boolean... oldStyle) {
        return create(varName, key, secret, null, null, target, isOldStyle(oldStyle));
    }

    public static KubernetesEnvBuildItem createFromResourceKey(String varName, String key, String secret,
            String configmap, String target, boolean... oldStyle) {
        return create(varName, key, secret, configmap, null, target, isOldStyle(oldStyle));
    }

    public static KubernetesEnvBuildItem create(String name, String value, String secret, String configmap, String field,
            String target, boolean... oldStyle) throws IllegalArgumentException {
        final boolean secretPresent = secret != null;
        final boolean configmapPresent = configmap != null;
        final boolean valuePresent = value != null;
        final boolean fieldPresent = field != null;
        if (valuePresent) {
            if (secretPresent && configmapPresent) {
                throw new IllegalArgumentException(String.format(
                        "'%s' env var can't simultaneously take its value from '%s' configmap & '%s' secret",
                        name, configmap, secret));
            }
            if (fieldPresent) {
                throw new IllegalArgumentException(String.format(
                        "'%s' env var can't simultaneously have a '%s' value & take is value from the '%s' field",
                        name, value, field));
            }
        }
        if (secretPresent && configmapPresent) {
            log.warn(String.format("The '%s' name was used to try to import both from '%s' secret & '%s' configmap. " +
                    "Only values from '%s' secret will be imported.\nIf you want to import from both, use a " +
                    "different property name for either.",
                    name, secret,
                    configmap,
                    secret));
        }
        final EnvType type;
        if (secretPresent) {
            if (valuePresent) {
                type = EnvType.keyFromSecret;
            } else {
                name = secret;
                type = EnvType.secret;
            }
        } else if (configmapPresent) {
            if (valuePresent) {
                type = EnvType.keyFromConfigmap;
            } else {
                name = configmap;
                type = EnvType.configmap;
            }
        } else if (field != null) {
            type = EnvType.field;
        } else {
            type = EnvType.var;
        }
        return new KubernetesEnvBuildItem(name, value, configmap, secret, field, type, target, isOldStyle(oldStyle));
    }

    private static boolean isOldStyle(boolean[] oldStyle) {
        return oldStyle.length >= 1 && oldStyle[0];
    }

    KubernetesEnvBuildItem(String name, String value, String configmap, String secret, String field, EnvType type,
            String target, boolean oldStyle) {
        this.name = name;
        this.value = value;
        this.configmap = configmap;
        this.secret = secret;
        this.field = field;
        this.type = type;
        this.target = target;
        this.oldStyle = oldStyle;
    }

    public String getConfigMap() {
        return configmap;
    }

    public String getSecret() {
        return secret;
    }

    public String getField() {
        return field;
    }

    public boolean isOldStyle() {
        return oldStyle;
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

    public KubernetesEnvBuildItem newWithTarget(String newTarget) {
        return new KubernetesEnvBuildItem(this.name, this.value, this.configmap, this.secret, this.field, this.type, newTarget,
                this.oldStyle);
    }

    public String toString() {
        switch (type) {
            case var:
                return String.format("'%s' env var with value '%s'", name, value);
            case field:
                return String.format("'%s' env var with value from field '%s'", name, field);
            case secret:
                return "all values from '" + secret + "' secret";
            case configmap:
                return "all values from '" + configmap + "' configmap";
            case keyFromConfigmap:
                return String.format("'%s' env var with value from '%s' key of '%s' configmap", name, value, configmap);
            case keyFromSecret:
                return String.format("'%s' env var with value from '%s' key of '%s' secret", name, value, secret);
            default:
                return "unknown type '" + type + "'";
        }
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
        if (value != null ? !value.equals(that.value) : that.value != null)
            return false;
        if (configmap != null ? !configmap.equals(that.configmap) : that.configmap != null)
            return false;
        if (secret != null ? !secret.equals(that.secret) : that.secret != null)
            return false;
        if (field != null ? !field.equals(that.field) : that.field != null)
            return false;
        return type == that.type;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + (configmap != null ? configmap.hashCode() : 0);
        result = 31 * result + (secret != null ? secret.hashCode() : 0);
        result = 31 * result + (field != null ? field.hashCode() : 0);
        result = 31 * result + type.hashCode();
        return result;
    }
}
