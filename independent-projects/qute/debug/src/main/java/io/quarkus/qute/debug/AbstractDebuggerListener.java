package io.quarkus.qute.debug;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public abstract class AbstractDebuggerListener extends UnicastRemoteObject implements DebuggerListener {

    protected AbstractDebuggerListener() throws RemoteException {
        super();
    }

}
