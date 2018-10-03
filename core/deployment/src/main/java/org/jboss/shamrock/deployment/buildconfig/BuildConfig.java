package org.jboss.shamrock.deployment.buildconfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/**
 * The build time configuration
 */
public class BuildConfig {

    private static final String META_INF_SHAMROCK_BUILD_YAML = "META-INF/shamrock-build.yaml";

    private static final ConfigNode NULL = new ConfigNode(null);

    private final ConfigNode applicationConfig;
    private final Map<URL, ConfigNode> dependencyConfig;

    BuildConfig(ConfigNode applicationConfig, Map<URL, ConfigNode> dependencyConfig) {
        this.applicationConfig = applicationConfig;
        this.dependencyConfig = Collections.unmodifiableMap(dependencyConfig);
    }

    public ConfigNode getApplicationConfig() {
        return applicationConfig;
    }

    public Map<URL, ConfigNode> getDependencyConfig() {
        return dependencyConfig;
    }

    public List<ConfigNode> getAll(String key) {
        List<ConfigNode> ret = new ArrayList<>();
        if (applicationConfig.containsKey(key)) {
            ret.add(applicationConfig.get(key));
        }
        for (Map.Entry<URL, ConfigNode> entry : dependencyConfig.entrySet()) {
            if (entry.getValue().containsKey(key)) {
                ret.add(entry.getValue().get(key));
            }
        }
        return ret;
    }


    public static BuildConfig readConfig(ClassLoader classLoader, File classesRoot) throws IOException {

        Yaml yaml = new Yaml(new SafeConstructor());
        ConfigNode app = NULL;
        File classesRootConfig = new File(classesRoot, META_INF_SHAMROCK_BUILD_YAML);
        if (classesRootConfig.exists()) {

            try (FileInputStream in = new FileInputStream(classesRootConfig)) {
                Object node = yaml.load(new InputStreamReader(in));
                app = new ConfigNode(node);
            }
        }
        Map<URL, ConfigNode> depConfig = new HashMap<>();
        Enumeration<URL> urls = classLoader.getResources(META_INF_SHAMROCK_BUILD_YAML);
        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            if (url.getProtocol().equals("file")) {
                if (new File(url.getPath()).equals(classesRootConfig)) {
                    continue;
                }
            }

            try (InputStream in = url.openStream()) {
                Object node = yaml.load(new InputStreamReader(in));
                depConfig.put(url, new ConfigNode(node));
            }
        }
        return new BuildConfig(app, depConfig);
    }


    /**
     * A wrapper around the unmarshalled yaml to make it simpler to work with in client code
     */
    public static class ConfigNode {

        private final Object node;

        public ConfigNode(Object node) {
            this.node = node;
        }

        public boolean containsKey(String key) {
            if (node instanceof Map) {
                return ((Map) node).containsKey(key);
            }
            return false;
        }

        public ConfigNode get(String key) {
            if (node instanceof Map) {
                Object val = ((Map) node).get(key);
                if (val != null) {
                    return new ConfigNode(val);
                }
                return NULL;
            }
            return NULL;
        }

        public Set<String> getChildKeys() {
            if (node instanceof Map) {
                return ((Map) node).keySet();
            }
            return Collections.emptySet();
        }

        public Boolean asBoolean() {
            if (node instanceof Boolean) {
                return (Boolean) node;
            }
            return null;
        }

        public String asString() {
            if (node instanceof String) {
                return (String) node;
            }
            return null;
        }

        public List<String> asStringList() {
            if (node instanceof List) {
                List<String> ret = new ArrayList<>();
                for (Object i : (List) node) {
                    ret.add(i.toString());
                }
                return ret;
            }
            return Collections.emptyList();
        }

        public List<ConfigNode> asConfigNodeList() {
            if (node instanceof List) {
                List<ConfigNode> ret = new ArrayList<>();
                for (Object i : (List) node) {
                    ret.add(new ConfigNode(i));
                }
                return ret;
            }
            return Collections.emptyList();
        }

        public Object getUnderlying() {
            return node;
        }

        public boolean isNull() {
            return node == null;
        }
    }
}
