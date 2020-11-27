package org.jboss.resteasy.reactive.spi;

public interface ThreadSetupAction {

    ThreadState activateInitial();

    interface ThreadState {
        void close();

        void activate();

        void deactivate();
    }

    ThreadSetupAction NOOP = new ThreadSetupAction() {
        @Override
        public ThreadState activateInitial() {
            return new ThreadState() {
                @Override
                public void close() {

                }

                @Override
                public void activate() {

                }

                @Override
                public void deactivate() {

                }
            };
        }
    };
}
