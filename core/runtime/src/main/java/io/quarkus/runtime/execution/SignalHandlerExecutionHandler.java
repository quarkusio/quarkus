package io.quarkus.runtime.execution;

import java.io.Serializable;

import org.graalvm.nativeimage.ImageInfo;

import io.quarkus.runtime.graal.DiagnosticPrinter;
import sun.misc.Signal;
import sun.misc.SignalHandler;

/**
 * The execution handler which sets up and tears down the basic execution services.
 */
public final class SignalHandlerExecutionHandler implements ExecutionHandler, Serializable {
    private static final long serialVersionUID = -4427286452713119540L;

    private static final String DISABLE_SIGNAL_HANDLERS = "DISABLE_SIGNAL_HANDLERS";

    static final SignalHandler EXIT_SIGNAL_HANDLER = new SignalHandler() {
        @Override
        public void handle(final Signal signal) {
            Execution.requestExit(signal.getNumber() + Execution.EXIT_SIGNAL_BASE);
        }
    };
    static final SignalHandler RELOAD_SIGNAL_HANDLER = new SignalHandler() {
        public void handle(final Signal sig) {
            Execution.requestExit(Execution.EXIT_RELOAD_FULL);
        }
    };
    static final Signal SIGINT = getSignal("INT");
    static final Signal SIGTERM = getSignal("TERM");
    static final Signal SIGHUP = getSignal("HUP");
    static final Signal SIGQUIT = getSignal("QUIT");

    private final boolean disableSignals;
    private final boolean restartOnHup;

    public SignalHandlerExecutionHandler(Builder builder) {
        disableSignals = builder.isDisableSignals();
        restartOnHup = builder.isRestartOnHup();
    }

    public int run(final ExecutionChain chain, final ExecutionContext context) throws Exception {
        final SignalHandler sigInt;
        final SignalHandler sigTerm;
        final SignalHandler sigHup;
        final SignalHandler sigQuit;
        if (disableSignals) {
            sigInt = null;
            sigTerm = null;
            sigHup = null;
            sigQuit = null;
        } else {
            sigInt = handleSignal(SIGINT, EXIT_SIGNAL_HANDLER);
            sigTerm = handleSignal(SIGTERM, EXIT_SIGNAL_HANDLER);
            if (restartOnHup) {
                sigHup = handleSignal(SIGHUP, RELOAD_SIGNAL_HANDLER);
                if (sigHup == null) {
                    // todo: log it (will not restart on signal)
                }
            } else {
                sigHup = handleSignal(SIGHUP, EXIT_SIGNAL_HANDLER);
            }
            if (ImageInfo.inImageRuntimeCode()) {
                sigQuit = handleSignal(SIGQUIT, new SignalHandler() {
                    @Override
                    public void handle(final Signal signal) {
                        DiagnosticPrinter.printDiagnostics(System.out);
                    }
                });
            } else {
                sigQuit = null;
            }
        }
        try {
            return chain.proceed(context);
        } finally {
            if (sigInt != null) {
                handleSignal(SIGINT, sigInt);
            }
            if (sigTerm != null) {
                handleSignal(SIGTERM, sigTerm);
            }
            if (sigHup != null) {
                handleSignal(SIGHUP, sigHup);
            }
            if (sigQuit != null) {
                handleSignal(SIGQUIT, sigQuit);
            }
        }
    }

    static Signal getSignal(String name) {
        try {
            return new Signal(name);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    static SignalHandler handleSignal(final Signal signal, final SignalHandler handler) {
        if (signal == null) {
            return null;
        }
        try {
            return Signal.handle(signal, handler);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private boolean disableSignals;
        private boolean restartOnHup = true;

        Builder() {
            final String dsh = System.getenv(DISABLE_SIGNAL_HANDLERS);
            if (dsh != null && !dsh.isEmpty()) {
                disableSignals = true;
            }
        }

        public boolean isDisableSignals() {
            return disableSignals;
        }

        public Builder setDisableSignals(final boolean disableSignals) {
            this.disableSignals = disableSignals;
            return this;
        }

        public boolean isRestartOnHup() {
            return restartOnHup;
        }

        public Builder setRestartOnHup(final boolean restartOnHup) {
            this.restartOnHup = restartOnHup;
            return this;
        }

        public SignalHandlerExecutionHandler build() {
            return new SignalHandlerExecutionHandler(this);
        }
    }
}
