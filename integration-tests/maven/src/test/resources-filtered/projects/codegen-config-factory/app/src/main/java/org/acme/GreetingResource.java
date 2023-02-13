package org.acme;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.RestPath;

@Path("/runtime-config")
public class GreetingResource {

	@ConfigProperty(name = AcmeConstants.ACME_CONFIG_PROVIDER_PROP)
	String acmeConfigSourceProvider;
	@ConfigProperty(name = AcmeConstants.ACME_CONFIG_FACTORY_PROP)
	String acmeConfigSourceFactory;
	
    @GET
    @Path("{name}")
    public String hello(@RestPath String name) {
    	if(AcmeConstants.ACME_CONFIG_PROVIDER_PROP.equals(name)) {
    		return acmeConfigSourceProvider;
    	}
    	if(AcmeConstants.ACME_CONFIG_FACTORY_PROP.equals(name)) {
    		return acmeConfigSourceFactory;
    	}
        throw new IllegalArgumentException(name);
    }
}
