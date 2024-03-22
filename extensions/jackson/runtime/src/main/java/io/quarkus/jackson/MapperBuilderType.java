package io.quarkus.jackson;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.MapperBuilder;
import com.fasterxml.jackson.databind.json.JsonMapper;

/**
 * Currently supported {@link ObjectMapper} {@link com.fasterxml.jackson.databind.cfg.MapperBuilder} types.
 */
@SuppressWarnings("rawtypes")
public enum MapperBuilderType {
    JSON(
            "com.fasterxml.jackson.databind.json.JsonMapper",
            "com.fasterxml.jackson.core:jackson-databind",
            JsonMapper::builder),
    YAML(
            "com.fasterxml.jackson.dataformat.yaml.YAMLMapper",
            "com.fasterxml.jackson.dataformat:jackson-dataformat-yaml"),
    JAVAPROPS(
            "com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper",
            "com.fasterxml.jackson.dataformat:jackson-dataformat-properties"),
    TOML(
            "com.fasterxml.jackson.dataformat.toml.TomlMapper",
            "com.fasterxml.jackson.dataformat:jackson-dataformat-toml"),
    CBOR(
            "com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper",
            "com.fasterxml.jackson.dataformat:jackson-dataformat-cbor"),
    SMILE(
            "com.fasterxml.jackson.dataformat.smile.databind.SmileMapper",
            "com.fasterxml.jackson.dataformat:jackson-dataformat-smile");

    private final String mapperClass;
    private final String dependency;
    private final Supplier<MapperBuilder> supplier;

    MapperBuilderType(String mapperClass, String dependency) {
        this(mapperClass, dependency, null);
    }

    MapperBuilderType(String mapperClass, String dependency, Supplier<MapperBuilder> supplier) {
        this.mapperClass = mapperClass;
        this.dependency = dependency;
        this.supplier = supplier;
    }

    public String getMapperClass() {
        return mapperClass;
    }

    public String getDependency() {
        return dependency;
    }

    public Supplier<MapperBuilder> getSupplier() {
        return Objects.requireNonNullElseGet(supplier, () -> () -> {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            try {
                Class<?> clazz = cl.loadClass(mapperClass);
                Method builder = clazz.getMethod("builder");
                return (MapperBuilder) builder.invoke(null);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });
    }
}
