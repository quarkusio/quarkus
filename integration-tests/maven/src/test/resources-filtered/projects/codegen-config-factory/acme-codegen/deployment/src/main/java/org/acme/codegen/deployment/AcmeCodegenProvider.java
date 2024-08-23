package org.acme.codegen.deployment;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import org.acme.AcmeConstants;
import org.eclipse.microprofile.config.Config;

import io.quarkus.bootstrap.prebuild.CodeGenException;
import io.quarkus.deployment.CodeGenContext;
import io.quarkus.deployment.CodeGenProvider;

public class AcmeCodegenProvider implements CodeGenProvider {

	private static final String ACME = "acme";

	@Override
	public String providerId() {
		return ACME;
	}

	@Override
	public String inputExtension() {
		return ACME;
	}

	@Override
	public String inputDirectory() {
		return ACME;
	}

	@Override
	public boolean trigger(CodeGenContext context) throws CodeGenException {
		generateEndpoint(context, AcmeConstants.ACME_CONFIG_FACTORY_PROP);
		generateEndpoint(context, AcmeConstants.ACME_CONFIG_PROVIDER_PROP);
		return true;
	}
	
	@Override
	public boolean shouldRun(Path sourceDir, Config config) {
		return !sourceDir.getParent().getFileName().toString().equals("generated-test-sources");
	}

	private void generateEndpoint(CodeGenContext ctx, String propName) throws CodeGenException {
		try {
			generateEndpoint(ctx.outDir(), propName, ctx.config().getOptionalValue(propName, String.class).orElse(AcmeConstants.NA));
		} catch (IOException e) {
			throw new CodeGenException("Failed to generate sources", e);
		}
	}
	
	private static void generateEndpoint(Path outputDir, String name, String value) throws IOException {
		
		final StringBuilder sb = new StringBuilder();
		boolean nextUpperCase = true;
		for(int i = 0; i < name.length(); ++i) {
			char c = name.charAt(i);
			if(c == '-') {
				nextUpperCase = true;
			} else if(nextUpperCase) {
				sb.append(Character.toUpperCase(c));
				nextUpperCase = false;
			} else {
				sb.append(c);
			}
		}
		sb.append("Resource");
		
		final String className = sb.toString();
		final Path javaFile = outputDir.resolve("org").resolve(ACME).resolve(className + ".java");
		Files.createDirectories(javaFile.getParent());
		try(PrintWriter out = new PrintWriter(Files.newBufferedWriter(javaFile))) {
			out.println("package org.acme;");
			out.println("import jakarta.ws.rs.GET;");
			out.println("import jakarta.ws.rs.Path;");
			out.println("@Path(\"/codegen-config\")");
			out.println("public class " + className + " {");
			out.println("  @GET");
			out.println("  @Path(\"/" + name + "\")");
			out.println("  public String main() {");
			out.println("    return \"" + value + "\";");
			out.println("  }");
			out.println("}");
		}
	}
}
