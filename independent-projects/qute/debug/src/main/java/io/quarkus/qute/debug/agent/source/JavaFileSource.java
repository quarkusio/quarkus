package io.quarkus.qute.debug.agent.source;

import java.net.URI;

public class JavaFileSource extends FileSource {

    public JavaFileSource(URI uri, String templateId, int startLine) {
        super(uri, templateId, startLine);
    }

}
