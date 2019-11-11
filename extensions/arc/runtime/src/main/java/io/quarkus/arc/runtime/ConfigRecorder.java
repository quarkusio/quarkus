package io.quarkus.arc.runtime;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.enterprise.inject.spi.DeploymentException;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.runtime.annotations.Recorder;

/**
 * @author Martin Kouba
 */
@Recorder
public class ConfigRecorder {

    public void validateConfigProperties(Map<String, Set<String>> properties) {
        Config config = ConfigProvider.getConfig();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = ConfigRecorder.class.getClassLoader();
        }
        for (Entry<String, Set<String>> entry : properties.entrySet()) {
            Set<String> propertyTypes = entry.getValue();
            for (String propertyType : propertyTypes) {
                Class<?> propertyClass = load(propertyType, cl);
                // For parameterized types and arrays, we only check if the property config exists without trying to convert it
                if (propertyClass.isArray() || propertyClass.getTypeParameters().length > 0) {
                    propertyClass = String.class;
                }
                try {
                    if (!config.getOptionalValue(entry.getKey(), propertyClass).isPresent()) {
                        throw new DeploymentException(
                                "No config value of type " + entry.getValue() + " exists for: " + entry.getKey());
                    }
                } catch (IllegalArgumentException e) {
                    throw new DeploymentException(e);
                }
            }
        }
    }

    private Class<?> load(String className, ClassLoader cl) {
        switch (className) {
            case "boolean":
                return boolean.class;
            case "byte":
                return byte.class;
            case "short":
                return short.class;
            case "int":
                return int.class;
            case "long":
                return long.class;
            case "float":
                return float.class;
            case "double":
                return double.class;
            case "char":
                return char.class;
            case "void":
                return void.class;
        }
        try {
            return Class.forName(className, true, cl);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Unable to load the config property type: " + className, e);
        }
    }

}
