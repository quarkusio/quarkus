package io.quarkus.qute.debug.server;

import io.quarkus.qute.trace.ResolveEvent;
import io.quarkus.qute.trace.TemplateEvent;
import io.quarkus.qute.trace.TraceListener;
import java.rmi.RemoteException;

public class DebuggerTraceListener implements TraceListener {

    private final RemoteDebuggerServer remoteDebuggerServer;

    public DebuggerTraceListener(RemoteDebuggerServer remoteDebuggerServer) {
        this.remoteDebuggerServer = remoteDebuggerServer;
    }

    @Override
    public void beforeResolve(ResolveEvent event) {

    }

    @Override
    public void afterResolve(ResolveEvent event) {
        try {
            remoteDebuggerServer.onTemplateNode(event);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void startTemplate(TemplateEvent event) {
        try {
            remoteDebuggerServer.onStartTemplate(event);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void endTemplate(TemplateEvent event) {
        try {
            remoteDebuggerServer.onEndTemplate(event);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

}
