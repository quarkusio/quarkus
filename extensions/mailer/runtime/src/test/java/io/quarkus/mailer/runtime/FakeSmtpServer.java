package io.quarkus.mailer.runtime;

import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import io.vertx.core.net.JksOptions;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.parsetools.RecordParser;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.net.NetServer;
import io.vertx.mutiny.core.net.NetSocket;

public class FakeSmtpServer {

    private NetServer netServer;
    private String[][] dialogue;
    private boolean closeImmediately = false;
    private int closeWaitTime = 10;

    private final boolean ssl;
    private String keystore;

    /*
     * set up server with a default reply that works for EHLO and no login with one recipient
     */
    public FakeSmtpServer(Vertx vertx, boolean ssl, String keystore) {
        setDialogue("220 example.com ESMTP",
                "EHLO",
                "250-example.com\n"
                        + "250-SIZE 1000000\n"
                        + "250 PIPELINING",
                "MAIL FROM:",
                "250 2.1.0 Ok",
                "RCPT TO:",
                "250 2.1.5 Ok",
                "DATA",
                "354 End data with <CR><LF>.<CR><LF>",
                "250 2.0.0 Ok: queued as ABCDDEF0123456789",
                "QUIT",
                "221 2.0.0 Bye");
        this.ssl = ssl;
        this.keystore = keystore;
        startServer(vertx);
    }

    private void startServer(Vertx vertx) {
        NetServerOptions nsOptions = new NetServerOptions();
        int port = ssl ? 1465 : 1587;
        nsOptions.setPort(port);
        if (keystore == null) {
            keystore = "src/test/resources/certs/server2.jks";
        }
        JksOptions jksOptions = new JksOptions().setPath(keystore).setPassword("password");
        nsOptions.setKeyStoreOptions(jksOptions);
        if (ssl) {
            nsOptions.setSsl(true);
        }
        netServer = vertx.createNetServer(nsOptions);

        netServer.connectHandler(socket -> {
            writeResponses(socket, dialogue[0]);
            if (dialogue.length == 1) {
                if (closeImmediately) {
                    socket.closeAndForget();
                } else {
                    vertx.setTimer(closeWaitTime * 1000L, v -> socket.closeAndForget());
                }
            } else {
                final AtomicInteger lines = new AtomicInteger(1);
                final AtomicInteger skipUntilDot = new AtomicInteger(0);
                final AtomicBoolean holdFire = new AtomicBoolean(false);
                final AtomicInteger inputLineIndex = new AtomicInteger(0);
                socket.handler(b -> RecordParser.newDelimited("\r\n", buffer -> {
                    final String inputLine = buffer.toString();
                    if (skipUntilDot.get() == 1) {
                        if (inputLine.equals(".")) {
                            skipUntilDot.set(0);
                            if (!holdFire.get() && lines.get() < dialogue.length) {
                                writeResponses(socket, dialogue[lines.getAndIncrement()]);
                            }
                        }
                    } else {
                        int currentLine = lines.get();
                        if (currentLine < dialogue.length) {
                            boolean inputValid = false;
                            holdFire.compareAndSet(false, true);
                            if (inputLineIndex.get() < dialogue[currentLine].length) {
                                String thisLine = dialogue[currentLine][inputLineIndex.get()];
                                boolean isRegexp = thisLine.startsWith("^");
                                if (!isRegexp && inputLine.contains(thisLine) || isRegexp && inputLine.matches(thisLine)) {
                                    inputValid = true;
                                    if (inputLineIndex.get() == dialogue[currentLine].length - 1) {
                                        holdFire.compareAndSet(true, false);
                                        lines.getAndIncrement();
                                        inputLineIndex.set(0);
                                    } else {
                                        inputLineIndex.getAndIncrement();
                                    }
                                }
                            }
                            if (!inputValid) {
                                socket.writeAndForget("500 didn't expect commands (\"" + String.join(",", dialogue[currentLine])
                                        + "\"/\"" + inputLine + "\")\r\n");
                                // stop here
                                lines.set(dialogue.length);
                            }
                        } else {
                            socket.writeAndForget("500 out of lines\r\n");
                        }
                        if (inputLine.toUpperCase(Locale.ENGLISH).equals("DATA")) {
                            skipUntilDot.set(1);
                        }
                        if (!holdFire.get() && inputLine.toUpperCase(Locale.ENGLISH).equals("STARTTLS")) {
                            writeResponses(socket, dialogue[lines.getAndIncrement()]);
                        } else if (!holdFire.get() && lines.get() < dialogue.length) {
                            writeResponses(socket, dialogue[lines.getAndIncrement()]);
                        }
                        if (inputLine.equals("QUIT")) {
                            socket.closeAndForget();
                        }
                    }
                    if (lines.get() == dialogue.length) {
                        if (closeImmediately) {
                            socket.closeAndForget();
                        } else {
                            vertx.setTimer(closeWaitTime * 1000L, v -> socket.closeAndForget());
                        }
                    }
                }).handle(b.getDelegate()));
            }
        });

        netServer.listenAndAwait();
    }

    private void writeResponses(NetSocket socket, String[] responses) {
        for (String line : responses) {
            socket.writeAndForget(line + "\r\n");
        }
    }

    public FakeSmtpServer setDialogue(String... dialogue) {
        this.dialogue = new String[dialogue.length][1];
        for (int i = 0; i < dialogue.length; i++) {
            this.dialogue[i] = new String[] { dialogue[i] };
        }
        return this;
    }

    /**
     * Sets the dialogue array.
     *
     * This is useful in case of pipelining is supported to group commands and responses.
     *
     * @param dialogue the dialogues
     * @return a reference to this, so the API can be used fluently
     */
    public FakeSmtpServer setDialogueArray(String[][] dialogue) {
        this.dialogue = dialogue;
        return this;
    }

    public FakeSmtpServer setCloseImmediately(boolean close) {
        closeImmediately = close;
        return this;
    }

    public FakeSmtpServer setCloseWaitTime(int time) {
        closeWaitTime = time;
        return this;
    }

    public void stop() {
        if (netServer != null) {
            CountDownLatch latch = new CountDownLatch(1);
            netServer.closeAndAwait();
            netServer = null;
        }
    }

}
