package toremove2;

import io.quarkus.qute.debug.AbstractDebuggerListener;
import io.quarkus.qute.debug.Breakpoint;
import io.quarkus.qute.debug.StoppedEvent;
import io.quarkus.qute.debug.ThreadEvent;
import io.quarkus.qute.debug.client.RemoteDebuggerClient;
import java.rmi.RemoteException;

public class RemoteDebuggerClientTest {

    private static class Listener extends AbstractDebuggerListener {

        protected Listener() throws RemoteException {
            super();
        }

        @Override
        public void onStopped(StoppedEvent event) throws RemoteException {
            System.err.println("Stopped event thread id:" + event.getThreadId());
        }

        @Override
        public void onThreadChanged(ThreadEvent event) throws RemoteException {
            System.err.println("Thread event thread id:" + event.getThreadId() + " [" + event.getThreadStatus() + "]");
        }

        @Override
        public void onTerminate() {
            System.err.println("Terminate");
        }
    };

    public static void main(String[] args) throws Exception {
        RemoteDebuggerClient client = RemoteDebuggerClient.connect();
        client.addDebuggerListener(new Listener());

        Breakpoint breakpoint = client.setBreakpoint(QuteRender.HELLO_TEMPLATE, 1);
        System.err.println(breakpoint.getLine());
        Thread.sleep(1000);

        io.quarkus.qute.debug.Thread[] threads = client.getThreads();
        io.quarkus.qute.debug.Thread thread = threads[0];
        long threadId = thread.getId();
        String threadName = thread.getName();
        System.err.println(threadId + ":" + threadName);

        Thread.sleep(1000);
        client.stepIn(threadId);
        Thread.sleep(1000);
        client.resume(threadId);
    }
}
