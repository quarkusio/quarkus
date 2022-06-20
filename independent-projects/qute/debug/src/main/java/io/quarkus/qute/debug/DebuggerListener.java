package io.quarkus.qute.debug;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface DebuggerListener extends Remote {

    void onStopped(StoppedEvent event) throws RemoteException;

    void onThreadChanged(ThreadEvent event) throws RemoteException;

    void onTerminate() throws RemoteException;

}
