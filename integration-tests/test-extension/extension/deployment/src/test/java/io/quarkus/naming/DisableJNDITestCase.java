package io.quarkus.naming;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Hashtable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class DisableJNDITestCase {

    @RegisterExtension
    static QuarkusUnitTest unitTest = new QuarkusUnitTest();

    @Test
    public void testJNDIDisabled() throws Exception {
        CompletableFuture<Boolean> cf = new CompletableFuture<>();
        //we just want to test that there is a connection attempt
        //we open a socket and will fail the test if anything actually manages
        //to connect to it
        int port = 0;
        try (ServerSocket s = new ServerSocket(0)) {
            port = s.getLocalPort();
            System.out.println("Bound server to " + port);
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        s.accept().close();
                        cf.complete(true);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            t.setDaemon(true);
            t.start();
            final int finalPort = port;
            Assertions.assertThrows(NamingException.class, () -> {
                new InitialContext().lookup("ldap://127.0.0.1:" + finalPort + "/a");
            });
            Assertions.assertThrows(NamingException.class, () -> {
                //now test explicitly setting the initial context
                Hashtable<Object, Object> environment = new Hashtable<>();
                environment.put(Context.INITIAL_CONTEXT_FACTORY, "javax.naming.ldap.InitialLdapContext");
                new InitialContext(environment).lookup("ldap://127.0.0.1:" + finalPort + "/a");
            });
            Assertions.assertThrows(TimeoutException.class, () -> {
                cf.get(1, TimeUnit.SECONDS);
            });
        }
    }
}
