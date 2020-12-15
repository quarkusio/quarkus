package io.quarkus.vertx.http.runtime.devmode.devconsole;

import java.time.Duration;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;

public abstract class DevConsolePostHandler implements Handler<RoutingContext> {

    @Override
    public void handle(RoutingContext event) {
        event.request().setExpectMultipart(true);
        event.request().endHandler(new Handler<Void>() {
            @Override
            public void handle(Void ignore) {
                dispatch(event);
            }
        });
    }

    protected void dispatch(RoutingContext event) {
        MultiMap form = event.request().formAttributes();
        try {
            handlePost(event, form);
            actionSuccess(event);
            return;
        } catch (NotImplementedException e) {
            // just go on and do async
        } catch (Exception e) {
            event.fail(e);
        }

        try {
            new Thread(new Runnable() {
                public void run() {
                    try {
                        handlePostAsync(event, form);
                        actionSuccess(event);
                    } catch (Exception e) {
                        event.fail(e);
                    }
                }
            }, "DEV Console action").start();
            return;
        } catch (NotImplementedException e) {
            throw new RuntimeException("One of handlePost or handleAsyncPost must be implemented");
        } catch (Exception e) {
            event.fail(e);
        }
    }

    private void actionSuccess(RoutingContext event) {
        event.response().setStatusCode(HttpResponseStatus.SEE_OTHER.code()).headers()
                .set(HttpHeaderNames.LOCATION, event.request().absoluteURI());
        event.response().end();
    }

    protected void flashMessage(RoutingContext event, String message) {
        FlashScopeUtil.setFlashMessage(event, message);
    }

    protected void flashMessage(RoutingContext event, String message, Duration displayTime) {
        FlashScopeUtil.setFlashMessage(event, message, displayTime);
    }

    protected void flashMessage(RoutingContext event, String message, FlashScopeUtil.FlashMessageStatus messageStatus) {
        FlashScopeUtil.setFlashMessage(event, message, messageStatus, null);
    }

    private static class NotImplementedException extends RuntimeException {
    }

    protected void handlePost(RoutingContext event, MultiMap form) throws Exception {
        throw new NotImplementedException();
    }

    protected void handlePostAsync(RoutingContext event, MultiMap form) throws Exception {
        throw new NotImplementedException();
    }
}
