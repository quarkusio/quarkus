import io.quarkus.maven.it.assertions.SetupVerifier

String base = basedir
File pomFile = new File(base, "pom.xml")

SetupVerifier.verifySetup(pomFile)
