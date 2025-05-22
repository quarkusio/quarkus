package io.quarkus.devui.runtime;

import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.impl.RouteImpl;

@ApplicationScoped
public class VertxRouteInfoService {

    private Router router;

    public void init(@Observes Router router) {
        this.router = router;
    }

    // There must be a better way to get this...
    public JsonArray getInfo() {

        JsonArray allRoutes = new JsonArray();
        for (Route route : router.getRoutes()) {
            RouteImpl routeImpl = ((RouteImpl) route);
            String routeImplJson = routeImpl.toString();
            int begin = routeImplJson.lastIndexOf("{") + 1;
            int end = routeImplJson.indexOf("}");
            routeImplJson = routeImplJson.substring(begin, end);
            String fields[] = routeImplJson.split(",");
            Map<String, Object> routeMap = new HashMap<>();
            for (String field : fields) {
                String kv[] = field.split("=");
                String key = kv[0];
                String value = "";
                if (kv.length > 1) {
                    value = kv[1];
                }
                routeMap.put(key.trim(), value.trim());
            }
            allRoutes.add(new JsonObject(routeMap));
        }
        return allRoutes;
    }
}
