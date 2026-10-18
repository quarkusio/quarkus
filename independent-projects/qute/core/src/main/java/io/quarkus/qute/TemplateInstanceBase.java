package io.quarkus.qute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.jboss.logging.Logger;

public abstract class TemplateInstanceBase implements TemplateInstance {

    private static final Logger LOG = Logger.getLogger(TemplateInstanceBase.class);

    static final String DATA_MAP_KEY = "io.quarkus.qute.dataMap";
    static final Map<String, Object> EMPTY_DATA_MAP = Collections.singletonMap(DATA_MAP_KEY, true);

    protected Object data;
    protected DataMap dataMap;
    protected final Map<String, Object> attributes;
    protected List<Runnable> renderedActions;

    public TemplateInstanceBase() {
        this.attributes = new HashMap<>();
    }

    @Override
    public TemplateInstance data(Object data) {
        this.data = data;
        dataMap = null;
        return this;
    }

    @Override
    public TemplateInstance data(String key, Object data) {
        this.data = null;
        if (dataMap == null) {
            dataMap = new DataMap();
        }
        dataMap.put(Objects.requireNonNull(key), data);
        return this;
    }

    @Override
    public TemplateInstance computedData(String key, Function<String, Object> function) {
        this.data = null;
        if (dataMap == null) {
            dataMap = new DataMap();
        }
        dataMap.computed(Objects.requireNonNull(key), Objects.requireNonNull(function));
        return this;
    }

    @Override
    public TemplateInstance setAttribute(String key, Object value) {
        attributes.put(key, value);
        return this;
    }

    @Override
    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    @Override
    public TemplateInstance onRendered(Runnable action) {
        if (renderedActions == null) {
            renderedActions = new ArrayList<>();
        }
        renderedActions.add(action);
        return this;
    }

    @Override
    public long getTimeout() {
        return attributes.isEmpty() ? engine().getTimeout() : getTimeoutAttributeValue();
    }

    private long getTimeoutAttributeValue() {
        Object t = getAttribute(TemplateInstance.TIMEOUT);
        if (t != null) {
            if (t instanceof Long) {
                return ((Long) t).longValue();
            } else {
                try {
                    return Long.parseLong(t.toString());
                } catch (NumberFormatException e) {
                    LOG.warnf("Invalid timeout value set for " + toString() + ": " + t);
                }
            }
        }
        return engine().getTimeout();
    }

    protected Object data() {
        if (data != null) {
            return data;
        }
        if (dataMap != null) {
            return dataMap;
        }
        return EMPTY_DATA_MAP;
    }

    protected abstract Engine engine();

    public static class DataMap implements Mapper {

        private final Map<String, Object> map = new HashMap<>();
        private Map<String, Function<String, Object>> computations = null;

        void put(String key, Object value) {
            map.put(key, value);
        }

        void computed(String key, Function<String, Object> function) {
            if (!map.containsKey(key)) {
                if (computations == null) {
                    computations = new HashMap<>();
                }
                computations.put(key, function);
            }
        }

        @Override
        public Object get(String key) {
            Object val = map.get(key);
            if (val == null) {
                if (key.equals(DATA_MAP_KEY)) {
                    return true;
                } else if (computations != null) {
                    Function<String, Object> fun = computations.get(key);
                    if (fun != null) {
                        return fun.apply(key);
                    }
                }
            }
            return val;
        }

        @Override
        public boolean appliesTo(String key) {
            return map.containsKey(key) || (computations != null && computations.containsKey(key));
        }

        @Override
        public Set<String> mappedKeys() {
            Set<String> ret = new HashSet<>(map.keySet());
            if (computations != null) {
                ret.addAll(computations.keySet());
            }
            return ret;
        }

        public void forEachData(BiConsumer<String, Object> action) {
            map.forEach(action);
        }

        public void forEachComputedData(BiConsumer<String, Function<String, Object>> action) {
            if (computations != null) {
                computations.forEach(action);
            }
        }

    }

}
