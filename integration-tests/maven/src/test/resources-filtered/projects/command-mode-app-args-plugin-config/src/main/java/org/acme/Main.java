package org.acme;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class Main implements QuarkusApplication {

	@Override
	public int run(String... args) throws Exception {
		System.out.println("ARGS: " + Arrays.asList(args));
		
		final Path path = Paths.get("done.txt");
		if(Files.exists(path)) {
			Files.delete(path);
		}
		Files.createFile(path);
		return 10;
	}
}
