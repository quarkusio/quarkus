package org.acme;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class Printer {

	public static interface Message {
		
		String getMessage();
	}
	
	public void println(Message msg) {
		System.out.println(msg.getMessage());
	}
}
