package org.jboss.resteasy.reactive.server.vertx.test.simple;

import javax.ws.rs.QueryParam;
import org.junit.jupiter.api.Assertions;

public class BeanParamSubClass extends BeanParamSuperClass {
    @QueryParam("query")
    String queryInSubClass;

    public void check(String path) {
        super.check(path);
        Assertions.assertEquals("one-query", queryInSubClass);
    }
}
