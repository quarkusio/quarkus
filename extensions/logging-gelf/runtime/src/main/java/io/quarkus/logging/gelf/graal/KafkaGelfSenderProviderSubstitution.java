package io.quarkus.logging.gelf.graal;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import biz.paluch.logging.gelf.intern.GelfSender;
import biz.paluch.logging.gelf.intern.GelfSenderConfiguration;

@TargetClass(className = "biz.paluch.logging.gelf.intern.sender.KafkaGelfSenderProvider")
public final class KafkaGelfSenderProviderSubstitution {

    @Substitute
    public GelfSender create(GelfSenderConfiguration configuration) {
        return null;
    }

}
