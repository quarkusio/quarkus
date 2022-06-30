package io.quarkus.it.opentelemetry.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Help send junk to the HTTP server. Something that is not possible with restassured.
 */
public class SocketClient implements AutoCloseable {
    private Socket socket;
    private PrintWriter out;

    private BufferedReader bufIn;

    private InputStreamReader readIn;

    public SocketClient(String ip, int port) throws IOException {
        socket = new Socket(ip, port);
        socket.setSoTimeout(1000);// making sure we don't hang the build
        socket.setKeepAlive(false);
        out = new PrintWriter(socket.getOutputStream(), true);
        readIn = new InputStreamReader(socket.getInputStream());
        bufIn = new BufferedReader(readIn);
    }

    public String sendMessage(String msg) throws IOException {
        out.println(msg);
        String resp = bufIn.readLine();
        return resp;
    }

    @Override
    public void close() throws IOException {
        bufIn.close();
        out.close();
        readIn.close();
        socket.close();
    }
}
