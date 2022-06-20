package io.quarkus.qute.debug;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Debugger extends Remote {

    public static final String DEBUGGER_NAME = "QuteDebugger";

    public static final int DEFAULT_PORT = 7022;

    /**
     * Returns the remote debugger state.
     *
     * @return the remote debugger state.
     */
    DebuggerState getState(long threadId) throws RemoteException;

    void pause(long threadId) throws RemoteException;

    void resume(long threadId) throws RemoteException;

    Breakpoint getBreakpoint(String templateId, int line) throws RemoteException;

    Breakpoint setBreakpoint(String templateId, int line) throws RemoteException;

    Thread[] getThreads() throws RemoteException;

    Thread getThread(long threadId) throws RemoteException;

    StackTrace getStackTrace(long threadId) throws RemoteException;

    /**
     * Returns the variable scopes for the given stackframe ID <code>frameId</code>.
     *
     * @param frameId the stackframe ID
     *
     *
     * @return the variable scopes for the given stackframe ID <code>frameId</code>.
     * @throws RemoteException
     */
    Scope[] getScopes(int frameId) throws RemoteException;

    /**
     * Retrieves all child variables for the given variable reference.
     *
     * @param variablesReference the Variable reference.
     * @return all child variables for the given variable reference.
     *
     * @throws RemoteException
     */
    Variable[] getVariables(int variablesReference) throws RemoteException;

    void terminate() throws RemoteException;

    void stepIn(long threadId) throws RemoteException;

    void stepOut(long threadId) throws RemoteException;

    void stepOver(long threadId) throws RemoteException;

    void addDebuggerListener(DebuggerListener listener) throws RemoteException;

    void removeDebuggerListener(DebuggerListener listener) throws RemoteException;

}
