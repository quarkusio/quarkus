package org.acme.libb;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class LibB{

	public String getName(){
		return "LibB";
	}
}