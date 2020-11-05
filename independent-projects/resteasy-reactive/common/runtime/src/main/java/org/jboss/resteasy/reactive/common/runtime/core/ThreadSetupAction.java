package org.jboss.resteasy.reactive.common.runtime.core;

public interface ThreadSetupAction {

    ThreadState activateInitial();

    interface ThreadState {
        void close();

        void activate();

        void deactivate();
    }
}
