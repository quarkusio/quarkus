package io.quarkus.qute.debug.server.scopes;

import io.quarkus.qute.Mapper;
import io.quarkus.qute.ResolutionContext;
import io.quarkus.qute.debug.Variable;
import io.quarkus.qute.debug.server.RemoteScope;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class GlobalsScope extends RemoteScope {

    private final ResolutionContext context;

    public GlobalsScope(ResolutionContext context) {
        super("Globals");
        this.context = context;
    }

    @Override
    protected Collection<Variable> createVariables() {
        Collection<Variable> variables = new ArrayList<>();
        Object data = context != null ? context.getData() : null;
        Map<?, ?> dataMap = getMap(data);
        if (dataMap != null) {
            for (Map.Entry<?, ?> entry : dataMap.entrySet()) {
                String name = entry.getKey().toString();
                String type = null;
                String s = null;
                Object value = entry.getValue();
                if (value != null) {
                    s = value.toString();
                    type = value.getClass().getSimpleName();
                } else {
                    s = "null";
                }
                variables.add(new Variable(name, s, type));
            }
        }
        return variables;
    }

    private static Map<?, ?> getMap(Object data) {
        if (data == null) {
            return null;
        }
        if (data instanceof Map<?, ?>) {
            return (Map<?, ?>) data;
        }
        if (data instanceof Mapper) {
            Mapper mapper = (Mapper) data;
            try {
                Field field = mapper.getClass().getDeclaredFields()[0];
                field.setAccessible(true);
                return (Map<?, ?>) field.get(mapper);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }

}
