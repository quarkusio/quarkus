package io.quarkus.it.arc;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/request-scoped")
public class TestRequestScopeEndpoint {

    @Inject
    RequestScopedBean requestScopedBean;

    @GET
    public String manualValidation() {

        requestScopedBean.incrementAndGet();
        requestScopedBean.incrementAndGet();
        return "" + requestScopedBean.incrementAndGet();

    }

}
