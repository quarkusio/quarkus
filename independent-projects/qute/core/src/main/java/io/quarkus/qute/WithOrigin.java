package io.quarkus.qute;

import io.quarkus.qute.TemplateNode.Origin;

public interface WithOrigin {

    Origin getOrigin();
}