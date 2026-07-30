import io.quarkus.devtools.docs.VerifyDeveloperDocs

tasks.register<VerifyDeveloperDocs>("verifyDeveloperDocs") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Verifies the durable Gradle developer documentation."
    docsDirectory.set(layout.projectDirectory.dir("docs"))
}
