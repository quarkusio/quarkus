package org.acme.libb;
import org.acme.liba.LibA;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class LibB{

	@Inject
	LibA libA;

	public String getName(){
		return libA.getName();
	}
}