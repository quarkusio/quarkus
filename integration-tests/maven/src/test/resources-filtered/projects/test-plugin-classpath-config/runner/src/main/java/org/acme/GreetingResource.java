package org.acme;

import java.net.URL;
import java.util.Enumeration;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/hello")
public class GreetingResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
    	final Enumeration<URL> en;
        try {
            en = Thread.currentThread().getContextClassLoader().getResources("module");
        } catch(java.io.IOException e) {
            throw new IllegalStateException("Failed to locate 'module' resources on the classpath", e);
        }
        if(!en.hasMoreElements()) {
        	return "";
        }
        final StringBuilder sb = new StringBuilder();
        sb.append(getNextModuleName(en));
        while(en.hasMoreElements()) {
        	sb.append(',').append(getNextModuleName(en));
        }
        return sb.toString();
    }

	private String getNextModuleName(final Enumeration<URL> en) {
		String s = en.nextElement().toExternalForm();
		s = s.substring(0, s.length() - "module".length() - 1);
		if(s.endsWith("target/classes")) {
			s = s.substring(0, s.length() - "target/classes".length() - 1);
		}
		s = s.substring(s.lastIndexOf('/') + 1);
		return s;
	}
}
