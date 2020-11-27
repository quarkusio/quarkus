package org.jboss.resteasy.reactive.server.handlers;

import io.smallrye.mutiny.Multi;
import javax.ws.rs.core.MediaType;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.core.SseUtil;
import org.jboss.resteasy.reactive.server.core.StreamingUtil;
import org.jboss.resteasy.reactive.server.jaxrs.OutboundSseEventImpl;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class MultiResponseHandler implements ServerRestHandler {

    class SseMultiSubscriber extends AbstractMultiSubscriber {

        SseMultiSubscriber(ResteasyReactiveRequestContext requestContext) {
            super(requestContext);
        }

        @Override
        public void onNext(Object item) {
            OutboundSseEventImpl event = new OutboundSseEventImpl.BuilderImpl().data(item).build();
            SseUtil.send(requestContext, event).handle((v, t) -> {
                if (t != null) {
                    // need to cancel because the exception didn't come from the Multi
                    subscription.cancel();
                    handleException(requestContext, t);
                } else {
                    // send in the next item
                    subscription.request(1);
                }
                return null;
            });
        }
    }

    class StreamingMultiSubscriber extends AbstractMultiSubscriber {

        StreamingMultiSubscriber(ResteasyReactiveRequestContext requestContext) {
            super(requestContext);
        }

        @Override
        public void onNext(Object item) {
            StreamingUtil.send(requestContext, item).handle((v, t) -> {
                if (t != null) {
                    // need to cancel because the exception didn't come from the Multi
                    try {
                        subscription.cancel();
                    } catch (Throwable t2) {
                        t2.printStackTrace();
                    }
                    handleException(requestContext, t);
                } else {
                    // send in the next item
                    subscription.request(1);
                }
                return null;
            });
        }
    }

    abstract class AbstractMultiSubscriber implements Subscriber<Object> {
        protected Subscription subscription;
        protected ResteasyReactiveRequestContext requestContext;

        AbstractMultiSubscriber(ResteasyReactiveRequestContext requestContext) {
            this.requestContext = requestContext;
            // let's make sure we never restart by accident, also make sure we're not marked as completed
            requestContext.restart(AWOL, true);
        }

        @Override
        public void onSubscribe(Subscription s) {
            this.subscription = s;
            // initially ask for one item
            s.request(1);
        }

        @Override
        public void onComplete() {
            // no need to cancel on complete
            // FIXME: are we interested in async completion?
            requestContext.serverResponse().end();
            // so, if I don't also close the connection, the client isn't notified that the request is over
            // I guess that's true of chunked responses, but not clear why I need to close the connection
            // because it means it can't get reused, right?
            requestContext.serverRequest().closeConnection();
            requestContext.close();
        }

        @Override
        public void onError(Throwable t) {
            // no need to cancel on error
            handleException(requestContext, t);
        }
    }

    private static final Logger log = Logger.getLogger(MultiResponseHandler.class);

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
        if (requestContext.getResult() instanceof Multi) {
            Multi<?> result = (Multi<?>) requestContext.getResult();
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
            if (requestContext.getResponseContentType().getMediaType().isCompatible(MediaType.SERVER_SENT_EVENTS_TYPE)) {
                handleSse(requestContext, result);
            } else {
                handleStreaming(requestContext, result);
            }
        }
    }

    private void handleStreaming(ResteasyReactiveRequestContext requestContext, Multi<?> result) {
        result.subscribe().withSubscriber(new StreamingMultiSubscriber(requestContext));
    }

    private void handleSse(ResteasyReactiveRequestContext requestContext, Multi<?> result) {
        result.subscribe().withSubscriber(new SseMultiSubscriber(requestContext));
    }

    private void handleException(ResteasyReactiveRequestContext requestContext, Throwable t) {
        // in truth we can only send an exception if we haven't sent the headers yet, otherwise
        // it will appear to be an SSE value, which is incorrect, so we should only log it and close the connection
        if (requestContext.serverResponse().headWritten()) {
            log.error("Exception in SSE server handling, impossible to send it to client", t);
        } else {
            // we can go through the abort chain
            requestContext.resume(t);
        }
    }
}
