package io.quarkus.remotedev;

import java.io.DataOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
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
                // read the file path separator of the remote endpoint
                final String filePathSeparator = new String(bytes, 1, bytes.length - 1, StandardCharsets.UTF_8);
                sendChangedClasses(filePathSeparator);
                break;
            }
            default: {
                logMessage("Ignoring unknown message type " + bytes[0]);
            }
        }
    }

    private void sendChangedClasses(final String remoteFilePathSeparator) {
        final Map<String, byte[]> changedSrcs = changedSrcs();
        final Map<String, byte[]> changedResources = changedWebResources();
        logMessage("Scan complete changed srcs " + changedSrcs.keySet()
                + " changes resources " + changedResources);
        final boolean filePathSeparatorsDiffer = !File.separator.equals(remoteFilePathSeparator);
        try (OutputStream out = session.getBasicRemote().getSendStream()) {

            out.write(CLASS_CHANGE_RESPONSE);
            DataOutputStream data = new DataOutputStream(new DeflaterOutputStream(out));
            data.writeInt(changedSrcs.size());
            for (Map.Entry<String, byte[]> entry : changedSrcs.entrySet()) {
                String path = entry.getKey();
                if (filePathSeparatorsDiffer) {
                    path = path.replace(File.separator, remoteFilePathSeparator);
                }
                data.writeUTF(path);
                data.writeInt(entry.getValue().length);
                data.write(entry.getValue());
            }
            data.writeInt(changedResources.size());
            for (Map.Entry<String, byte[]> entry : changedResources.entrySet()) {
                String path = entry.getKey();
                if (filePathSeparatorsDiffer) {
                    path = path.replace(File.separator, remoteFilePathSeparator);
                }
                data.writeUTF(path);
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
