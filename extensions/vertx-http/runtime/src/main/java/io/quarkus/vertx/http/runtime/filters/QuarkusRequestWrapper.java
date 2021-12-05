package io.quarkus.vertx.http.runtime.filters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.function.BiConsumer;

import org.jboss.logging.Logger;

import io.quarkus.vertx.http.runtime.AbstractRequestWrapper;
import io.vertx.core.Handler;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.CookieSameSite;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

public class QuarkusRequestWrapper extends AbstractRequestWrapper {

    /**
     * Huge hack, to work around the fact that there is no way to directly access this class once it is wrapped,
     * we use a fake cookie
     */
    public static final String FAKE_COOKIE_NAME = "X-quarkus-request-wrapper";

    private static final Logger log = Logger.getLogger(QuarkusRequestWrapper.class);

    private volatile int done;
    private static final AtomicIntegerFieldUpdater<QuarkusRequestWrapper> doneUpdater = AtomicIntegerFieldUpdater
            .newUpdater(QuarkusRequestWrapper.class, "done");

    private Handler<Throwable> exceptionHandler;
    private final AbstractResponseWrapper response;

    private final List<Handler<Void>> requestDoneHandlers = new ArrayList<>();
    private final BiConsumer<Cookie, HttpServerRequest> cookieConsumer;

    public QuarkusRequestWrapper(HttpServerRequest event, BiConsumer<Cookie, HttpServerRequest> cookieConsumer) {
        super(event);
        this.cookieConsumer = cookieConsumer;
        this.response = new ResponseWrapper(delegate.response(), event);
        event.exceptionHandler(new Handler<Throwable>() {
            @Override
            public void handle(Throwable event) {
                if (exceptionHandler != null) {
                    exceptionHandler.handle(event);
                }
                done();
            }
        });
    }

    public static QuarkusRequestWrapper get(HttpServerRequest request) {
        return ((QuarkusRequestWrapper.QuarkusCookie) request.getCookie(QuarkusRequestWrapper.FAKE_COOKIE_NAME))
                .getRequestWrapper();
    }

    public void addRequestDoneHandler(Handler<Void> handler) {
        this.requestDoneHandlers.add(handler);
    }

    @Override
    public HttpServerResponse response() {
        return response;
    }

    void done() {
        if (doneUpdater.compareAndSet(this, 0, 1)) {
            for (Handler<Void> i : requestDoneHandlers) {
                try {
                    i.handle(null);
                } catch (Throwable t) {
                    log.error("Failed to run " + i, t);
                }
            }
        }
    }

    @Override
    public HttpServerRequest exceptionHandler(Handler<Throwable> handler) {
        exceptionHandler = handler;
        return this;
    }

    @Override
    public Cookie getCookie(String name) {
        if (name.equals(FAKE_COOKIE_NAME)) {
            return new QuarkusCookie();
        }
        return super.getCookie(name);
    }

    @Override
    public Cookie getCookie(String name, String domain, String path) {
        if (name.equals(FAKE_COOKIE_NAME)) {
            return new QuarkusCookie();
        }
        return super.getCookie(name, domain, path);
    }

    @Override
    public Set<Cookie> cookies(String name) {
        if (name.equals(FAKE_COOKIE_NAME)) {
            return Collections.singleton(new QuarkusCookie());
        }
        return super.cookies(name);
    }

    class ResponseWrapper extends AbstractResponseWrapper {

        final HttpServerRequest request;
        Handler<Void> endHandler;
        Handler<Void> closeHandler;
        Handler<Throwable> exceptionHandler;

        ResponseWrapper(HttpServerResponse delegate, HttpServerRequest request) {
            super(delegate);
            this.request = request;
            delegate.closeHandler(new Handler<Void>() {
                @Override
                public void handle(Void event) {
                    done();
                    if (closeHandler != null) {
                        closeHandler.handle(event);
                    }
                }
            });
            delegate.exceptionHandler(new Handler<Throwable>() {
                @Override
                public void handle(Throwable event) {
                    done();

                    if (exceptionHandler != null) {
                        exceptionHandler.handle(event);
                    }
                }
            });
            delegate.endHandler(new Handler<Void>() {
                @Override
                public void handle(Void event) {
                    done();
                    if (endHandler != null) {
                        endHandler.handle(event);
                    }
                }
            });
        }

        @Override
        public HttpServerResponse exceptionHandler(Handler<Throwable> handler) {
            this.exceptionHandler = handler;
            return this;
        }

        @Override
        public HttpServerResponse closeHandler(Handler<Void> handler) {
            this.closeHandler = handler;
            return this;
        }

        @Override
        public HttpServerResponse endHandler(Handler<Void> handler) {
            this.endHandler = handler;
            return this;
        }

        @Override
        public HttpServerResponse addCookie(Cookie cookie) {
            if (cookieConsumer != null) {
                cookieConsumer.accept(cookie, request);
            }
            return super.addCookie(cookie);
        }
    }

    public class QuarkusCookie implements Cookie {

        public QuarkusRequestWrapper getRequestWrapper() {
            //it does not get much more hacky than this
            //hopefully we can work around it in the next Vert.x version
            return QuarkusRequestWrapper.this;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public String getValue() {
            return null;
        }

        @Override
        public Cookie setValue(String value) {
            return null;
        }

        @Override
        public Cookie setDomain(String domain) {
            return null;
        }

        @Override
        public String getDomain() {
            return null;
        }

        @Override
        public Cookie setPath(String path) {
            return null;
        }

        @Override
        public String getPath() {
            return null;
        }

        @Override
        public Cookie setMaxAge(long maxAge) {
            return null;
        }

        @Override
        public long getMaxAge() {
            return Long.MIN_VALUE;
        }

        @Override
        public Cookie setSecure(boolean secure) {
            return null;
        }

        @Override
        public Cookie setHttpOnly(boolean httpOnly) {
            return null;
        }

        @Override
        public Cookie setSameSite(CookieSameSite policy) {
            return null;
        }

        @Override
        public String encode() {
            return null;
        }

        @Override
        public CookieSameSite getSameSite() {
            return null;
        }

        @Override
        public boolean isSecure() {
            return false;
        }

        @Override
        public boolean isHttpOnly() {
            return false;
        }
    }

}
