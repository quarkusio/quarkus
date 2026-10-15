package io.quarkus.observation.handler;

import org.jboss.logging.Logger;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;

public class PrintOutHandler implements ObservationHandler<Observation.Context> {

    private static final Logger LOG = Logger.getLogger(PrintOutHandler.class);

    @Override
    public void onStart(Observation.Context context) {
        LOG.infof("START  name=%s, contextualName=%s, lowCardinalityKeyValues=%s, highCardinalityKeyValues=%s",
                context.getName(), context.getContextualName(),
                context.getLowCardinalityKeyValues(), context.getHighCardinalityKeyValues());
    }

    @Override
    public void onError(Observation.Context context) {
        LOG.infof("ERROR  name=%s, contextualName=%s, error=%s",
                context.getName(), context.getContextualName(), context.getError());
    }

    @Override
    public void onEvent(Observation.Event event, Observation.Context context) {
        LOG.infof("EVENT  name=%s, contextualName=%s, event=%s",
                context.getName(), context.getContextualName(), event.getName());
    }

    @Override
    public void onScopeOpened(Observation.Context context) {
        LOG.infof("SCOPE OPENED  name=%s, contextualName=%s",
                context.getName(), context.getContextualName());
    }

    @Override
    public void onScopeClosed(Observation.Context context) {
        LOG.infof("SCOPE CLOSED  name=%s, contextualName=%s",
                context.getName(), context.getContextualName());
    }

    @Override
    public void onStop(Observation.Context context) {
        LOG.infof("STOP   name=%s, contextualName=%s, lowCardinalityKeyValues=%s, highCardinalityKeyValues=%s",
                context.getName(), context.getContextualName(),
                context.getLowCardinalityKeyValues(), context.getHighCardinalityKeyValues());
    }

    @Override
    public boolean supportsContext(Observation.Context context) {
        return true;
    }
}
