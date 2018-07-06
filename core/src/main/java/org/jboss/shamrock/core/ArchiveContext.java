package org.jboss.shamrock.core;

import java.nio.file.Path;
import java.util.List;

import org.jboss.jandex.Index;

public interface ArchiveContext {

    Index getIndex();

    Path getArchiveRoot();

}
