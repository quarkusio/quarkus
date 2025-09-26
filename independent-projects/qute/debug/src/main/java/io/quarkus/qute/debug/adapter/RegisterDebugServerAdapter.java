package io.quarkus.qute.debug.adapter;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.eclipse.lsp4j.debug.launch.DSPLauncher;
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient;
import org.eclipse.lsp4j.jsonrpc.Launcher;

import io.quarkus.qute.Engine;
import io.quarkus.qute.EngineBuilder.EngineListener;
import io.quarkus.qute.debug.agent.DebuggeeAgent;
import io.quarkus.qute.trace.TemplateEvent;
import io.quarkus.qute.trace.TraceListener;

public class RegisterDebugServerAdapter implements EngineListener {

    // Port to listen for debug connections, retrieved from environment
    private Integer port;
    private Boolean suspend;

    // Engines being tracked by the debug agent
    private final Set<Engine> trackedEngines = new HashSet<>();

    // Engines that are debuggable but not yet initialized
    private final Set<Engine> notInitializedEngines = new HashSet<>();

    private volatile boolean initialized;
    private volatile DebuggeeAgent agent;
    private volatile ServerSocket serverSocket;
    private Future<Void> launcherFuture;
    private DebugServerAdapter server;

    private final ExecutorService executor = createDaemonExecutor();

    // Listener used to lazily initialize the debug agent when the first template is
    // rendered
    private final TraceListener initializeAgentListener = new TraceListener() {
        @Override
        public void onStartTemplate(TemplateEvent event) {
            // Trigger initialization when the first template starts rendering
            initializeAgent(getPort(), isSuspend());
        }
    };

    public RegisterDebugServerAdapter() {
        this(null, null);
    }

    public RegisterDebugServerAdapter(Integer port, Boolean suspend) {
        this.port = port;
        this.suspend = suspend;
    }

    @Override
    public void engineBuilt(Engine engine) {

        // Track the debuggable engine
        trackedEngines.add(engine);

        // If already initialized, immediately attach the engine
        if (initialized) {
            agent.track(engine);
            return;
        }

        Integer port = getPort();
        if (port == null) {
            return;
        }

        // Create the debug agent if needed
        agent = createAgentIfNeeded();

        // If not yet initialized, attach a listener to lazily initialize the agent
        // later
        initializeAgent(port, isSuspend());

        // Track the engine in the agent
        agent.track(engine);
    }

    private DebuggeeAgent createAgentIfNeeded() {
        if (agent == null) {
            synchronized (this) {
                if (agent == null) {
                    agent = new DebuggeeAgent();
                    server = new DebugServerAdapter(agent);
                    trackedEngines.forEach(agent::track);
                }
            }
        }
        return agent;
    }

    /**
     * Returns the Qute debugger port defines with "-DquteDebugPort" environment variable and false otherwise.
     *
     * @return the Qute debugger port defines with "-DquteDebugPort" environment variable and false otherwise.
     */
    public Integer getPort() {
        if (port != null) {
            return port;
        }
        port = doGetPort();
        return port;
    }

    private static Integer doGetPort() {
        // Read the debug port from the environment variable
        String portStr = getPropertyValue("quteDebugPort");
        if (portStr == null || portStr.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(portStr);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isSuspend() {
        if (suspend != null) {
            return suspend;
        }
        suspend = doIsSuspend();
        return suspend;
    }

    private boolean doIsSuspend() {
        // Read the suspend flag from the environment variable
        String suspend = getPropertyValue("quteDebugSuspend");
        if (suspend == null || suspend.isBlank()) {
            return false;
        }
        try {
            return Boolean.parseBoolean(suspend);
        } catch (Exception e) {
            return false;
        }
    }

    private static String getPropertyValue(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            value = System.getProperty(name);
        }
        return value;
    }

    /**
     * Initializes the debug agent. In suspend mode, this call blocks until a DAP
     * client connects. Otherwise, initialization is done asynchronously in a
     * background thread.
     */
    private synchronized void initializeAgent(int port, boolean suspend) {
        if (initialized) {
            return;
        }
        if (suspend) {
            // In suspend mode: block until a DAP client connects
            initializeAgentBlocking(port, true);
        } else {
            // In non-suspend mode: run in background without blocking main thread
            executor.execute(() -> initializeAgentBlocking(port, false));
        }
    }

    /**
     * Performs the blocking initialization of the debug agent, including waiting
     * for client connections depending on the suspend flag.
     *
     * @param port the port to listen on
     * @param suspend whether to block on accept or run accept loop in background
     */
    private synchronized void initializeAgentBlocking(int port, boolean suspend) {
        if (serverSocket != null) {
            return;
        }

        try {
            serverSocket = new ServerSocket(port);
            log("Qute debugger server listening on port " + serverSocket.getLocalPort());
            if (suspend) {
                // Suspend mode: block here until a DAP client connects
                log("Waiting for Qute debugger client to connect (suspend mode)...");
                var client = serverSocket.accept();
                log("Qute debugger client connected (suspend mode)!");
                setupLauncher(client, true);
            } else {
                // Non-suspend mode: accept clients asynchronously in a daemon thread loop
                executor.execute(() -> {
                    while (serverSocket != null && !serverSocket.isClosed()) {
                        try {
                            log("Waiting for a new Qute debugger client...");
                            var client = serverSocket.accept();
                            log("Qute debugger client connected");
                            setupLauncher(client, false);
                            trackedEngines.forEach(engine -> agent.track(engine));
                        } catch (IOException e) {
                            if (serverSocket != null && !serverSocket.isClosed()) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
            }

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                dispose();
            }));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void dispose() {
        if (launcherFuture != null && !launcherFuture.isDone()) {
            launcherFuture.cancel(true);
        }
        log("Shutdown hook: closing server socket.");
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            serverSocket = null;
        }
        // Shutdown executor service gracefully
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                log("Executor did not terminate in the specified time.");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        agent.terminate();
    }

    public void reset() {
        agent.reset();
        trackedEngines.clear();
        if (!notInitializedEngines.isEmpty()) {
            notInitializedEngines.forEach(engine -> {
                if (engine.getTraceManager() != null) {
                    engine.removeTraceListener(initializeAgentListener);
                }
            });
            notInitializedEngines.clear();
        }
    }

    /**
     * Creates and starts the DAP launcher for the connected client socket. Cancels
     * any previous launcher.
     *
     * @param client the connected socket
     * @param suspend true if suspend mode (used to wait after connection)
     * @throws IOException if socket streams cannot be accessed
     */
    private void setupLauncher(Socket client, boolean suspend) throws IOException {
        // Cancel previous launcher if needed
        if (launcherFuture != null && !launcherFuture.isDone()) {
            launcherFuture.cancel(true);
        }

        Launcher<IDebugProtocolClient> launcher = DSPLauncher.createServerLauncher(server, client.getInputStream(),
                client.getOutputStream(), executor, null);

        var clientProxy = launcher.getRemoteProxy();
        server.connect(clientProxy);
        launcherFuture = launcher.startListening();

        if (!notInitializedEngines.isEmpty()) {
            notInitializedEngines.forEach(engine -> {
                if (engine.getTraceManager() != null) {
                    engine.removeTraceListener(initializeAgentListener);
                }
            });
            notInitializedEngines.clear();
        }

        if (suspend) {
            // Sleep a bit to allow the client to register breakpoints
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private ExecutorService createDaemonExecutor() {
        return Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("dap-daemon-thread");
            return t;
        });
    }

    private static void log(String message) {
        System.out.println(message);
    }

}
