package org.jboss.shamrock.example.rest;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.eclipse.microprofile.metrics.annotation.Counted;
import org.jboss.shamrock.example.MessageBean;

@Path("/test")
public class InjectionResource {

	@Inject
	MessageBean messageBean;

	@GET
	@Counted(monotonic = true)
	public String getTest() {
		return "TEST";
	}

	@GET
	@Path("/injection")
	public String get() {
		return messageBean.getMessage();
	}

}
