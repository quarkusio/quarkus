package org.jboss.shamrock.core;

import java.nio.file.Path;
import java.util.List;

import org.jboss.jandex.Index;

public interface ArchiveContext {

    Index getIndex();

    Path getArchiveRoot();

    <T> T getAttachment(AttachmentKey<T> key);

    <T> void setAttachment(AttachmentKey<T> key, T value);

    <T> void addToList(ListAttachmentKey<T> key, T value);

    <T> List<T> getList(ListAttachmentKey<T> key);

}
