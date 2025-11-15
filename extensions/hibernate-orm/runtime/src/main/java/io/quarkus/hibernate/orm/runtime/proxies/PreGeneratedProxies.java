package io.quarkus.hibernate.orm.runtime.proxies;

import java.util.HashMap;
import java.util.Map;

/**
 * A holder class for proxies that were generated at build time,
 * where possible these are re-used rather than generating new ones
 * at static init time.
 *
 * In most circumstances these will be used for every entity, however
 * in some corner cases it may still be necessary to generate proxies
 * at static init time.
 *
 * This class is bytecode recordable.
 */
public class PreGeneratedProxies {

    private Map<String, ProxyClassDetailsHolder> proxies = new HashMap<>();

    public Map<String, ProxyClassDetailsHolder> getProxies() {
        return proxies;
    }

    public void setProxies(Map<String, ProxyClassDetailsHolder> proxies) {
        this.proxies = proxies;
    }

    public static class ProxyClassDetailsHolder {

        private String className;

        public ProxyClassDetailsHolder() {

        }

        public ProxyClassDetailsHolder(String className) {
            this.className = className;
        }

        public String getClassName() {
            return className;
        }

        public void setClassName(String className) {
            this.className = className;
        }

    }
}
