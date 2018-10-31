package org.jboss.shamrock.camel.runtime.graal;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

class JdkSubstitutions {
}

@TargetClass(className = "javax.xml.transform.TransformerException")
final class Target_javax_xml_transform_TransformerException {
    @Substitute
    public void printStackTrace(java.io.PrintWriter s) {
        s.print("Error omitted: javax.xml.transform.TransformerException is subtituted");
    }
}
