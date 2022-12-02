package io.quarkus.extest.runtime;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.logging.Logger;

import io.quarkus.extest.runtime.config.XmlConfig;

/**
 * Hypothetical legacy service that uses the XmlConfig configuration to run a socket based service
 */
public class RuntimeXmlConfigService {
    private static final Logger log = Logger.getLogger("RuntimeXmlConfigService");
    private XmlConfig config;
    private ServerSocket serverSocket;
    private volatile AtomicBoolean running = new AtomicBoolean(false);
    private Thread clientHandler;

    public RuntimeXmlConfigService(XmlConfig config) {
        this.config = config;
        log.infof("ctor, config: %s", config);
    }

    public void startService() throws IOException {
        log.infof("startService, config: %s", config);
        try {
            log.infof("Class.forName(XmlRootElement): %s", Class.forName("javax.xml.bind.annotation.XmlRootElement"));
        } catch (Exception e) {
            log.info("Failed to load XmlRootElement", e);
        }
        InetAddress address = InetAddress.getByName(config.getAddress());
        serverSocket = new ServerSocket(config.getPort(), -1, address);
        clientHandler = new Thread(this::run, "RuntimeXmlConfigServiceThread");
        clientHandler.setDaemon(false);
        running.set(true);
        clientHandler.start();
        log.info("startService, " + clientHandler);
    }

    public void stopService() {
        log.info("stopService, stopping...");
        running.set(false);
        try {
            serverSocket.close();
        } catch (IOException e) {
            log.warn("ServerSocket.close", e);
        }
        clientHandler.interrupt();
        log.info("stopService, complete.");
    }

    private void run() {
        try {
            log.info("Starting accept loop");
            while (running.get()) {
                Socket client = serverSocket.accept();
                log.infof("Accepted client: %s", client);
                CommandHandler handler = new CommandHandler(client.getInputStream(), client.getOutputStream());
                handler.run();
                client.close();
                log.infof("Closed client: %s", client);
            }
        } catch (Exception e) {
            if (!serverSocket.isClosed()) {
                log.warn("Error handling client request", e);
            }
        }
        log.info("Exiting accept loop");
    }

    private static class CommandHandler {
        private InputStream is;
        private OutputStream os;

        CommandHandler(InputStream inputStream, OutputStream outputStream) {
            this.is = inputStream;
            this.os = outputStream;
        }

        void run() throws IOException {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String command = reader.readLine();
            log.infof("Received command: %s", command);
            String reply = command + "-ack";
            os.write(reply.getBytes("UTF-8"));
        }
    }
}
