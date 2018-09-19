package org.jboss.shamrock.example.arc;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/request-scoped")
public class TestRequestScopeEndpoint {

    @Inject
    private RequestScopedBean requestScopedBean;

    @GET
    public String manualValidation() {

        requestScopedBean.incrementAndGet();
        requestScopedBean.incrementAndGet();
        return "" + requestScopedBean.incrementAndGet();

    }


}
