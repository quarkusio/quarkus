package io.quarkus.remotedev;

import java.io.DataOutputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.zip.DeflaterOutputStream;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

/**
 * Implements the Fakereplace Websocket protocol
 */
public abstract class QuarkusWebsocketProtocol extends Endpoint implements MessageHandler.Whole<byte[]> {
    private static final int CLASS_CHANGE_RESPONSE = 2;
    private static final int CLASS_CHANGE_REQUEST = 1;
    private volatile Session session;

    protected abstract Map<String, byte[]> changedSrcs();

    protected abstract Map<String, byte[]> changedWebResources();

    protected abstract void logMessage(String message);

    protected abstract void error(Throwable t);

    protected abstract void done();

    @Override
    public void onOpen(Session session, EndpointConfig endpointConfig) {
        logMessage("Connected to remote server");
        session.addMessageHandler(this);
        this.session = session;
    }

    @Override
    public void onClose(Session session, CloseReason closeReason) {
        logMessage("Connection closed " + closeReason);
        done();
    }

    @Override
    public void onError(Session session, Throwable thr) {
        error(thr);
    }

    @Override
    public void onMessage(byte[] bytes) {
        switch (bytes[0]) {
            case CLASS_CHANGE_REQUEST: {
                logMessage("Scanning for changed classes");
                sendChangedClasses();
                break;
            }
            default: {
                logMessage("Ignoring unknown message type " + bytes[0]);
            }
        }
    }

    private void sendChangedClasses() {
        final Map<String, byte[]> changedSrcs = changedSrcs();
        final Map<String, byte[]> changedResources = changedWebResources();
        logMessage("Scan complete changed srcs " + changedSrcs.keySet()
                + " changes resources " + changedResources);
        try (OutputStream out = session.getBasicRemote().getSendStream()) {

            out.write(CLASS_CHANGE_RESPONSE);
            DataOutputStream data = new DataOutputStream(new DeflaterOutputStream(out));
            data.writeInt(changedSrcs.size());
            for (Map.Entry<String, byte[]> entry : changedSrcs.entrySet()) {
                data.writeUTF(entry.getKey());
                data.writeInt(entry.getValue().length);
                data.write(entry.getValue());
            }
            data.writeInt(changedResources.size());
            for (Map.Entry<String, byte[]> entry : changedResources.entrySet()) {
                data.writeUTF(entry.getKey());
                data.writeInt(entry.getValue().length);
                data.write(entry.getValue());
            }
            data.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
