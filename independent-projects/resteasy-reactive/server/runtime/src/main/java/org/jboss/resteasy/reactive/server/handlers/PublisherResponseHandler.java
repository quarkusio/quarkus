package org.jboss.resteasy.reactive.server.handlers;

import java.nio.charset.StandardCharsets;
import java.util.function.BiFunction;
import javax.ws.rs.core.MediaType;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.core.SseUtil;
import org.jboss.resteasy.reactive.server.core.StreamingUtil;
import org.jboss.resteasy.reactive.server.jaxrs.OutboundSseEventImpl;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class PublisherResponseHandler implements ServerRestHandler {

    private static class SseMultiSubscriber extends AbstractMultiSubscriber {

        SseMultiSubscriber(ResteasyReactiveRequestContext requestContext) {
            super(requestContext);
        }

        @Override
        public void onNext(Object item) {
            OutboundSseEventImpl event = new OutboundSseEventImpl.BuilderImpl().data(item).build();
            SseUtil.send(requestContext, event).handle(new BiFunction<Object, Throwable, Object>() {
                @Override
                public Object apply(Object v, Throwable t) {
                    if (t != null) {
                        // need to cancel because the exception didn't come from the Multi
                        subscription.cancel();
                        handleException(requestContext, t);
                    } else {
                        // send in the next item
                        subscription.request(1);
                    }
                    return null;
                }
            });
        }
    }

    private static class StreamingMultiSubscriber extends AbstractMultiSubscriber {

        // Huge hack to stream valid json
        private boolean json;
        private String nextJsonPrefix;
        private boolean hadItem;

        StreamingMultiSubscriber(ResteasyReactiveRequestContext requestContext, boolean json) {
            super(requestContext);
            this.json = json;
            this.nextJsonPrefix = "[";
            this.hadItem = false;
        }

        @Override
        public void onNext(Object item) {
            hadItem = true;
            StreamingUtil.send(requestContext, item, json ? nextJsonPrefix : null)
                    .handle(new BiFunction<Object, Throwable, Object>() {
                        @Override
                        public Object apply(Object v, Throwable t) {
                            if (t != null) {
                                // need to cancel because the exception didn't come from the Multi
                                try {
                                    subscription.cancel();
                                } catch (Throwable t2) {
                                    t2.printStackTrace();
                                }
                                handleException(requestContext, t);
                            } else {
                                // next item will need this prefix if json
                                nextJsonPrefix = ",";
                                // send in the next item
                                subscription.request(1);
                            }
                            return null;
                        }
                    });
        }

        @Override
        public void onComplete() {
            if (!hadItem) {
                StreamingUtil.setHeaders(requestContext, requestContext.serverResponse());
            }
            if (json) {
                String postfix;
                // check if we never sent the open prefix
                if (!hadItem) {
                    postfix = "[]";
                } else {
                    postfix = "]";
                }
                byte[] postfixBytes = postfix.getBytes(StandardCharsets.US_ASCII);
                requestContext.serverResponse().write(postfixBytes).handle((v, t) -> {
                    super.onComplete();
                    return null;
                });
            } else {
                super.onComplete();
            }
        }
    }

    static abstract class AbstractMultiSubscriber implements Subscriber<Object> {
        protected Subscription subscription;
        protected ResteasyReactiveRequestContext requestContext;
        private boolean weClosed = false;

        AbstractMultiSubscriber(ResteasyReactiveRequestContext requestContext) {
            this.requestContext = requestContext;
            // let's make sure we never restart by accident, also make sure we're not marked as completed
            requestContext.restart(AWOL, true);
            requestContext.serverResponse().addCloseHandler(() -> {
                if (!weClosed && this.subscription != null) {
                    subscription.cancel();
                }
            });
        }

        @Override
        public void onSubscribe(Subscription s) {
            this.subscription = s;
            // initially ask for one item
            s.request(1);
        }

        @Override
        public void onComplete() {
            // make sure we don't trigger cancel with our onCloseHandler
            weClosed = true;
            // no need to cancel on complete
            // FIXME: are we interested in async completion?
            requestContext.serverResponse().end();
            requestContext.close();
        }

        @Override
        public void onError(Throwable t) {
            // no need to cancel on error
            handleException(requestContext, t);
        }

        protected void handleException(ResteasyReactiveRequestContext requestContext, Throwable t) {
            // in truth we can only send an exception if we haven't sent the headers yet, otherwise
            // it will appear to be an SSE value, which is incorrect, so we should only log it and close the connection
            if (requestContext.serverResponse().headWritten()) {
                log.error("Exception in SSE server handling, impossible to send it to client", t);
            } else {
                // we can go through the abort chain
                requestContext.resume(t, true);
            }
        }
    }

    private static final Logger log = Logger.getLogger(PublisherResponseHandler.class);

    private static final ServerRestHandler[] AWOL = new ServerRestHandler[] {
            new ServerRestHandler() {

                @Override
                public void handle(ResteasyReactiveRequestContext requestContext)
                        throws Exception {
                    throw new IllegalStateException("FAILURE: should never be restarted");
                }
            }
    };

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) throws Exception {
        // FIXME: handle Response with entity being a Multi
        if (requestContext.getResult() instanceof Publisher) {
            Publisher<?> result = (Publisher<?>) requestContext.getResult();
            // FIXME: if we make a pretend Response and go through the normal route, we will get
            // media type negotiation and fixed entity writer set up, perhaps it's better than
            // cancelling the normal route?
            // or make this SSE produce build-time
            MediaType[] mediaTypes = requestContext.getTarget().getProduces().getSortedMediaTypes();
            if (mediaTypes.length != 1)
                throw new IllegalStateException(
                        "Negotiation or dynamic media type not supported yet for Multi: please use a single @Produces annotation");
            requestContext.setResponseContentType(mediaTypes[0]);
            // this is the non-async return type
            requestContext.setGenericReturnType(requestContext.getTarget().getReturnType());
            // we have several possibilities here, but in all we suspend
            requestContext.suspend();
            if (mediaTypes[0].isCompatible(MediaType.SERVER_SENT_EVENTS_TYPE)) {
                handleSse(requestContext, result);
            } else {
                boolean json = mediaTypes[0].isCompatible(MediaType.APPLICATION_JSON_TYPE);
                handleStreaming(requestContext, result, json);
            }
        }
    }

    private void handleStreaming(ResteasyReactiveRequestContext requestContext, Publisher<?> result, boolean json) {
        result.subscribe(new StreamingMultiSubscriber(requestContext, json));
    }

    private void handleSse(ResteasyReactiveRequestContext requestContext, Publisher<?> result) {
        result.subscribe(new SseMultiSubscriber(requestContext));
    }
}
