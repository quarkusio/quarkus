package org.acme.testlib;

import org.acme.Printer;

public class TestMessage implements Printer.Message {

	private final String msg;
	
	public TestMessage(String msg) {
		this.msg = msg;
	}
	
	@Override
	public String getMessage() {
		return msg;
	}
}
