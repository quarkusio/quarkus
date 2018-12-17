package org.jboss.panache;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import io.reactivex.Maybe;
import io.reactivex.Single;

public class RxController {

    protected <T> Single<T> throwNotFoundIfNull(Maybe<T> maybe, String message) {
        return maybe.switchIfEmpty(Single.defer(() -> Single.error(new WebApplicationException(message, Status.NOT_FOUND))));
    }

    protected Single<Response> created(Single<? extends Object> single) {
        return single.map(value -> Response.ok(value).status(Status.CREATED).build());
    }
    
    protected Response noContent() {
        return Response.status(Status.NO_CONTENT).build();
    }
}
