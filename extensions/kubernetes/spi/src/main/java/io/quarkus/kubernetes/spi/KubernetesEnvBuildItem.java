
package io.quarkus.kubernetes.spi;

import java.util.Objects;

import org.jboss.logging.Logger;

public final class KubernetesEnvBuildItem extends BaseTargetable {
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

            return switch (this) {
                case field -> type == var || type == keyFromConfigmap || type == keyFromSecret;
                case var -> type == field || type == keyFromConfigmap || type == keyFromSecret;
                case secret -> type == configmap;
                case configmap -> type == secret;
                case keyFromConfigmap -> type == field || type == var || type == keyFromSecret;
                case keyFromSecret -> type == field || type == var || type == keyFromConfigmap;
            };
        }
    }

    private final String name;
    private final String value;
    private final String configmap;
    private final String secret;
    private final String field;
    private final EnvType type;
    private final boolean oldStyle;
    private final String prefix;

    public static KubernetesEnvBuildItem createFromField(String name, String targetField, String target,
            boolean... oldStyle) {
        return create(name, null, null, null, targetField, target, null, isOldStyle(oldStyle));
    }

    public static KubernetesEnvBuildItem createFromConfigMap(String configMapName, String target, String prefix,
            boolean... oldStyle) {
        return create(configMapName, null, null, configMapName, null, target, prefix, isOldStyle(oldStyle));
    }

    public static KubernetesEnvBuildItem createFromSecret(String secretName, String target, String prefix,
            boolean... oldStyle) {
        return create(secretName, null, secretName, null, null, target, prefix, isOldStyle(oldStyle));
    }

    public static KubernetesEnvBuildItem createSimpleVar(String name, String value, String target,
            boolean... oldStyle) {
        return create(name, value, null, null, null, target, null, isOldStyle(oldStyle));
    }

    public static KubernetesEnvBuildItem createFromConfigMapKey(String varName, String key, String configmap, String target,
            String prefix, boolean... oldStyle) {
        return create(varName, key, null, configmap, null, target, prefix, isOldStyle(oldStyle));
    }

    @SuppressWarnings("unused")
    public static KubernetesEnvBuildItem createFromSecretKey(String varName, String key, String secret, String target,
            String prefix, boolean... oldStyle) {
        return create(varName, key, secret, null, null, target, prefix, isOldStyle(oldStyle));
    }

    public static KubernetesEnvBuildItem createFromResourceKey(String varName, String key, String secret,
            String configmap, String target, boolean... oldStyle) {
        return create(varName, key, secret, configmap, null, target, null, isOldStyle(oldStyle));
    }

    public static KubernetesEnvBuildItem create(String name, String value, String secret, String configmap, String field,
            String target, String prefix, boolean... oldStyle) throws IllegalArgumentException {
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
        return new KubernetesEnvBuildItem(name, value, configmap, secret, field, type, target, prefix, isOldStyle(oldStyle));
    }

    private static boolean isOldStyle(boolean[] oldStyle) {
        return oldStyle.length >= 1 && oldStyle[0];
    }

    KubernetesEnvBuildItem(String name, String value, String configmap, String secret, String field, EnvType type,
            String target, String prefix, boolean oldStyle) {
        super(target);
        this.name = name;
        this.value = value;
        this.configmap = configmap;
        this.secret = secret;
        this.field = field;
        this.type = type;
        this.prefix = prefix;
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

    public String getPrefix() {
        return prefix;
    }

    @SuppressWarnings("unused")
    public KubernetesEnvBuildItem newWithTarget(String newTarget) {
        return new KubernetesEnvBuildItem(this.name, this.value, this.configmap, this.secret, this.field, this.type, newTarget,
                this.prefix, this.oldStyle);
    }

    public String toString() {
        return switch (type) {
            case var -> String.format("'%s' env var with value '%s'", name, value);
            case field -> String.format("'%s' env var with value from field '%s'", name, field);
            case secret -> "all values from '" + secret + "' secret";
            case configmap -> "all values from '" + configmap + "' configmap";
            case keyFromConfigmap ->
                String.format("'%s' env var with value from '%s' key of '%s' configmap", name, value, configmap);
            case keyFromSecret ->
                String.format("'%s' env var with value from '%s' key of '%s' secret", name, value, secret);
        };
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
        if (!Objects.equals(value, that.value))
            return false;
        if (!Objects.equals(configmap, that.configmap))
            return false;
        if (!Objects.equals(secret, that.secret))
            return false;
        if (!Objects.equals(field, that.field))
            return false;
        if (!Objects.equals(prefix, that.prefix))
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
        result = 31 * result + (prefix != null ? prefix.hashCode() : 0);
        return result;
    }
}
