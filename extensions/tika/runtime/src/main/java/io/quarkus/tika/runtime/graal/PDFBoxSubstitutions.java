package io.quarkus.tika.runtime.graal;

import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB")
final class Target_org_apache_pdfbox_pdmodel_graphics_color_PDDeviceRGB {
    @Substitute
    private void init() {
        // This method appears to be just a workaround for PDFBOX-2184
    }

    // awtColorSpace is not actually used in PDDeviceRGB
    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.Reset)
    private volatile ColorSpace awtColorSpace;
}

@TargetClass(className = "org.apache.pdfbox.pdmodel.graphics.color.PDICCBased")
final class Target_org_apache_pdfbox_pdmodel_graphics_color_PDICCBased {
    // This class provides alternative paths for when awtColorSpace is null, so it is safe to reset it
    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.Reset)
    private volatile ICC_ColorSpace awtColorSpace;
}

// Substitutions to prevent ICC_ColorSpace instances from appearing in the native image when using Apache Tika
// See https://github.com/quarkusio/quarkus/pull/13644
// These substitutions can be removed when moving from GraalVM 20.3 to GraalVM 21.0.
class PDFBoxSubstitutions {

}
