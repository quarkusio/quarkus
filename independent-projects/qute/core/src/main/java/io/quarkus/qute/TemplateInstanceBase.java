package io.quarkus.qute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jboss.logging.Logger;

public abstract class TemplateInstanceBase implements TemplateInstance {

    private static final Logger LOG = Logger.getLogger(TemplateInstanceBase.class);

    static final String DATA_MAP_KEY = "io.quarkus.qute.dataMap";
    static final Map<String, Object> EMPTY_DATA_MAP = Collections.singletonMap(DATA_MAP_KEY, true);

    protected Object data;
    protected Map<String, Object> dataMap;
    protected final Map<String, Object> attributes;
    protected final List<Runnable> renderedActions;

    public TemplateInstanceBase() {
        this.attributes = new HashMap<>();
        this.renderedActions = new ArrayList<>();
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
            dataMap = new HashMap<String, Object>();
            dataMap.put(DATA_MAP_KEY, true);
        }
        dataMap.put(key, data);
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
        renderedActions.add(action);
        return this;
    }

    @Override
    public long getTimeout() {
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
            return Mapper.wrap(dataMap);
        }
        return EMPTY_DATA_MAP;
    }

    protected abstract Engine engine();

}
