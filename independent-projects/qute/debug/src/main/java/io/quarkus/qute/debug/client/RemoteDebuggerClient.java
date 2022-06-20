package io.quarkus.qute.debug.client;

import io.quarkus.qute.debug.Breakpoint;
import io.quarkus.qute.debug.Debugger;
import io.quarkus.qute.debug.DebuggerException;
import io.quarkus.qute.debug.DebuggerListener;
import io.quarkus.qute.debug.DebuggerState;
import io.quarkus.qute.debug.DebuggerStoppedException;
import io.quarkus.qute.debug.Scope;
import io.quarkus.qute.debug.StackTrace;
import io.quarkus.qute.debug.Thread;
import io.quarkus.qute.debug.Variable;
import java.io.EOFException;
import java.net.SocketException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;

public class RemoteDebuggerClient implements Debugger {

    public static final int DEFAULT_MAX_TRIES = 10;

    public static final int DEFAULT_DELAY_BETWEEN_TRIES = 200;

    private static final String URL = "rmi://127.0.0.1:{0}/" + Debugger.DEBUGGER_NAME;

    private final Debugger remoteDebugger;

    private Collection<DebuggerListener> clientListeners;

    public static RemoteDebuggerClient connect() throws Exception {
        return connect(Debugger.DEFAULT_PORT);
    }

    public static RemoteDebuggerClient connect(int port) throws Exception {
        return connect(port, DEFAULT_MAX_TRIES, DEFAULT_DELAY_BETWEEN_TRIES);
    }

    public static RemoteDebuggerClient connect(int port, int maxTries, int delayBetweenTries) throws Exception {
        int tries = 0;
        while (tries < maxTries) {
            tries++;
            try {
                Debugger remoteDebugger = (Debugger) Naming.lookup(MessageFormat.format(URL, String.valueOf(port)));
                return new RemoteDebuggerClient(remoteDebugger);
            } catch (Exception e) {
                if (tries >= maxTries) {
                    throw e;
                }
                try {
                    java.lang.Thread.sleep(delayBetweenTries);
                } catch (InterruptedException e1) {
                    java.lang.Thread.currentThread().interrupt();
                }
            }
        }
        return null;
    }

    private RemoteDebuggerClient(Debugger remoteDebugger) {
        this.remoteDebugger = remoteDebugger;
        this.clientListeners = new ArrayList<>();
    }

    @Override
    public DebuggerState getState(long threadId) {
        try {
            return this.remoteDebugger.getState(threadId);
        } catch (RemoteException e) {
            throw throwException(e);
        }
    }

    @Override
    public void pause(long threadId) {
        try {
            this.remoteDebugger.pause(threadId);
        } catch (RemoteException e) {
            throw throwException(e);
        }
    }

    @Override
    public void resume(long threadId) {
        try {
            this.remoteDebugger.resume(threadId);
        } catch (RemoteException e) {
            throw throwException(e);
        }
    }

    @Override
    public Breakpoint getBreakpoint(String templateId, int line) {
        try {
            return this.remoteDebugger.getBreakpoint(templateId, line);
        } catch (RemoteException e) {
            throw throwException(e);
        }
    }

    @Override
    public Breakpoint setBreakpoint(String templateId, int line) {
        try {
            return this.remoteDebugger.setBreakpoint(templateId, line);
        } catch (RemoteException e) {
            throw throwException(e);
        }
    }

    @Override
    public Thread getThread(long threadId) throws RemoteException {
        try {
            return this.remoteDebugger.getThread(threadId);
        } catch (RemoteException e) {
            throw throwException(e);
        }
    }

    @Override
    public Thread[] getThreads() {
        try {
            return this.remoteDebugger.getThreads();
        } catch (RemoteException e) {
            throw throwException(e);
        }
    }

    @Override
    public void addDebuggerListener(DebuggerListener listener) throws RemoteException {
        try {
            clientListeners.add(listener);
            remoteDebugger.addDebuggerListener(listener);
        } catch (RemoteException e) {
            throw throwException(e);
        }
    }

    @Override
    public void removeDebuggerListener(DebuggerListener listener) throws RemoteException {
        try {
            clientListeners.remove(listener);
            remoteDebugger.removeDebuggerListener(listener);
        } catch (RemoteException e) {
            throw throwException(e);
        }
    }

    @Override
    public void terminate() {
        try {
            for (DebuggerListener listener : clientListeners) {
                remoteDebugger.removeDebuggerListener(listener);
            }
            clientListeners.clear();
        } catch (RemoteException e) {
            throw throwException(e);
        }
    }

    @Override
    public void stepIn(long threadId) {
        try {
            remoteDebugger.stepIn(threadId);
        } catch (RemoteException e) {
            throw throwException(e);
        }
    }

    @Override
    public void stepOut(long threadId) {
        try {
            remoteDebugger.stepOut(threadId);
        } catch (RemoteException e) {
            throw throwException(e);
        }
    }

    @Override
    public void stepOver(long threadId) {
        try {
            remoteDebugger.stepOver(threadId);
        } catch (RemoteException e) {
            throw throwException(e);
        }
    }

    @Override
    public StackTrace getStackTrace(long threadId) {
        try {
            return remoteDebugger.getStackTrace(threadId);
        } catch (RemoteException e) {
            throw throwException(e);
        }
    }

    @Override
    public Scope[] getScopes(int frameId) {
        try {
            return remoteDebugger.getScopes(frameId);
        } catch (RemoteException e) {
            throw throwException(e);
        }
    }

    @Override
    public Variable[] getVariables(int variablesReference) {
        try {
            return remoteDebugger.getVariables(variablesReference);
        } catch (RemoteException e) {
            throw throwException(e);
        }
    }

    private static RuntimeException throwException(RemoteException e) {
        Throwable cause = e.getCause();
        for (Throwable t = cause; t != null; t = t.getCause()) {
            if (t instanceof EOFException || t instanceof SocketException) {
                // Debugger is stopped
                throw new DebuggerStoppedException(t);
            }
        }
        if (cause instanceof RuntimeException) {
            throw (RuntimeException) cause;
        }
        return new DebuggerException(e);
    }

}
