package io.quarkus.bootstrap.resolver.maven;

import java.io.File;

interface LocalPomResolver {

    File resolvePom(String groupId, String artifactId, String version);
}
