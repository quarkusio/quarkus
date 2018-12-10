package org.jboss.shamrock.example.testutils;

import java.sql.SQLException;

import org.h2.tools.Server;

/**
 * JUnit helper to start the H2 database in server mode (over TCP)
 * to be able to test for actual database connection.
 *
 * @author Sanne Grinovero  <sanne@hibernate.org>
 */
public class H2DatabaseLifecycleManager extends org.junit.rules.ExternalResource {

   private int startedContainers = 0;
   private Server tcpServer;

   @Override
   public synchronized void before() {
      if (startedContainers==0) {
         try {
            tcpServer = Server.createTcpServer();
            tcpServer.start();
            startedContainers++;
            System.out.println("H2 database started in TCP server mode");
         } catch (SQLException e) {
            throw new RuntimeException(e);
         }
      }
   }

   @Override
   public synchronized void after() {
      startedContainers--;
      if (startedContainers==0 && tcpServer!=null) {
         tcpServer.stop();
         System.out.println("H2 database was shut down");
         tcpServer = null;
      }
   }

}
