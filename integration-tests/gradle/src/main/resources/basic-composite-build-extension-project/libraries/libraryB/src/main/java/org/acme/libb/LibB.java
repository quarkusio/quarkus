package org.acme.libb;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class LibB{

	public String getName(){
		return "LibB";
	}
}
