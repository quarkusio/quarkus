package io.quarkus.test.mongodb;

public class Issue14424 {
	
	public static void fix() {
		try {
			//JDK bug workaround
			//https://github.com/quarkusio/quarkus/issues/14424
			//force class init to prevent possible deadlock when done by mongo threads
			Class.forName("sun.net.ext.ExtendedSocketOptions", true, ClassLoader.getSystemClassLoader());
		} catch (ClassNotFoundException e) {
		}
	}
}
