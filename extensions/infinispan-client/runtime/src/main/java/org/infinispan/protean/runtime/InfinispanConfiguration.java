package org.infinispan.protean.runtime;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.shamrock.runtime.ConfigGroup;

/**
 * @author William Burns
 */
@ConfigGroup
public class InfinispanConfiguration {
   /**
    * Sets the host name to connect to
    */
   @ConfigProperty(name = "server_list")
   public String serverList;

   @Override
   public String toString() {
      return "InfinispanConfiguration{" +
            "serverList='" + serverList + '\'' +
            '}';
   }
}
