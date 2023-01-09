package org.acme.libb;
import org.acme.liba.LibA;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class LibB{

	@Inject
	LibA libA;

	public String getName(){
		return libA.getName();
	}
}
