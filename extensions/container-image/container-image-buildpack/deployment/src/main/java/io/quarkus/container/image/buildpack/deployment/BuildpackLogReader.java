package io.quarkus.container.image.buildpack.deployment;

import org.jboss.logging.Logger;

import dev.snowdrop.buildpack.BuildPackBuilder;

public class BuildpackLogReader implements BuildPackBuilder.LogReader {
    private static final Logger bplog = Logger.getLogger("buildpack");
    
	@Override
	public boolean stripAnsiColor() {
		return true;
	}
	
	private String trim(String message) {
		if(message.endsWith("\n")) {
			message = message.substring(0,message.length()-1);
		}
		if(message.endsWith("\r")) {
			message = message.substring(0,message.length()-1);
		}
		return message;
	}
	
	@Override
	public void stdout(String message) {
		
		bplog.info(trim(message));
	}
	
	@Override
	public void stderr(String message) {
		bplog.error(trim(message));
	}
}
