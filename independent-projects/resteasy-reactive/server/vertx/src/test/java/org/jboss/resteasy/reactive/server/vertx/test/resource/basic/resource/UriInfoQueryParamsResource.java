package org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;

@Path("UriInfoQueryParamsResource/queryParams")
public class UriInfoQueryParamsResource {
    @GET
    public String doGet(@QueryParam("a") String a, @Context UriInfo info) {
        Assertions.assertNotNull(info);

        Assertions.assertNotNull(info.getQueryParameters());
        assertNotMutable(info.getQueryParameters());

        return "content";
    }

    private static void assertNotMutable(MultivaluedMap<String, String> params) {

        final String param = "param";
        final String key = params.keySet().iterator().next();

        try {
            params.put(param, Collections.singletonList(param));
            Assertions.fail("mutable UriInfo");
        } catch (UnsupportedOperationException uoe) {
            //OK
        }

        try {
            params.add(param, param);
            Assertions.fail("mutable UriInfo");
        } catch (UnsupportedOperationException uoe) {
            //OK
        }

        try {
            params.addAll(param, Collections.singletonList(param));
            Assertions.fail("mutable UriInfo");
        } catch (UnsupportedOperationException uoe) {
            //OK
        }

        try {
            params.addAll(param, param);
            Assertions.fail("mutable UriInfo");
        } catch (UnsupportedOperationException uoe) {
            //OK
        }

        try {
            params.addFirst(param, param);
            Assertions.fail("mutable UriInfo");
        } catch (UnsupportedOperationException uoe) {
            //OK
        }

        try {
            params.putSingle(param, param);
            Assertions.fail("mutable UriInfo");
        } catch (UnsupportedOperationException uoe) {
            //OK
        }

        try {
            params.entrySet().add(new Map.Entry<String, List<String>>() {
                @Override
                public String getKey() {
                    return param;
                }

                @Override
                public List<String> getValue() {
                    return Collections.singletonList(param);
                }

                @Override
                public List<String> setValue(List<String> value) {
                    return Collections.singletonList(param);
                }
            });
            Assertions.fail("mutable UriInfo");
        } catch (UnsupportedOperationException uoe) {
            //OK
        }

        try {
            params.keySet().add(param);
            Assertions.fail("mutable UriInfo");
        } catch (UnsupportedOperationException uoe) {
            //OK
        }

        try {
            params.clear();
            Assertions.fail("mutable UriInfo");
        } catch (UnsupportedOperationException uoe) {
            //OK
        }

        try {
            params.putAll(params);
            Assertions.fail("mutable UriInfo");
        } catch (UnsupportedOperationException uoe) {
            //OK
        }

        try {
            params.remove(key);
            Assertions.fail("mutable UriInfo");
        } catch (UnsupportedOperationException uoe) {
            //OK
        }

        try {
            params.remove(null);
            Assertions.fail("mutable UriInfo");
        } catch (UnsupportedOperationException uoe) {
            //OK
        }

        try {
            params.values().add(Collections.singletonList(param));
            Assertions.fail("mutable UriInfo");
        } catch (UnsupportedOperationException uoe) {
            //OK
        }

    }
}
