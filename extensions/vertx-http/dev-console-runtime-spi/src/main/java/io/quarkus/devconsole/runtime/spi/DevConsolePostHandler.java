package io.quarkus.devconsole.runtime.spi;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;

public abstract class DevConsolePostHandler implements Handler<RoutingContext> {

    @Override
    public void handle(RoutingContext event) {
        if (event.getBody() != null) {
            //really simple form data decoder
            //but we can't really re-use the netty one
            String data = event.getBodyAsString();
            String[] parts = data.split("&");
            MultiMap post = MultiMap.caseInsensitiveMultiMap();
            for (String i : parts) {
                String[] pair = i.split("=");
                try {
                    post.add(URLDecoder.decode(pair[0], StandardCharsets.UTF_8.name()),
                            URLDecoder.decode(pair[1], StandardCharsets.UTF_8.name()));
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
            }
            dispatch(event, post);
        } else {
            event.request().setExpectMultipart(true);
            event.request().endHandler(new Handler<Void>() {
                @Override
                public void handle(Void ignore) {
                    dispatch(event, event.request().formAttributes());
                }
            });
        }
    }

    protected void dispatch(RoutingContext event, MultiMap form) {
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

    protected void actionSuccess(RoutingContext event) {
        if (!event.response().ended()) {
            event.response().setStatusCode(HttpResponseStatus.SEE_OTHER.code()).headers()
                    .set(HttpHeaderNames.LOCATION, event.request().absoluteURI());
            event.response().end();
        }
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
