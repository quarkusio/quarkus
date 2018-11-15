package org.jboss.shamrock.runtime.graal;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "javax.xml.transform.TransformerException")
public final class Target_javax_xml_TransformerException {

    @Substitute
    public void printStackTrace(java.io.PrintWriter s) {
        s.print("Error omitted: javax.xml.transform.TransformerException is substituted in Substrate");
    }

}
