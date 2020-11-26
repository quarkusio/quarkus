package org.jboss.resteasy.reactive.spi;

public interface ThreadSetupAction {

    ThreadState activateInitial();

    interface ThreadState {
        void close();

        void activate();

        void deactivate();
    }
}
