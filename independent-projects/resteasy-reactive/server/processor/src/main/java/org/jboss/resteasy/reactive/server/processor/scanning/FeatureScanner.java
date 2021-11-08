package org.jboss.resteasy.reactive.server.processor.scanning;

import java.util.List;
import org.jboss.jandex.IndexView;
import org.jboss.resteasy.reactive.server.processor.ScannedApplication;
import org.jboss.resteasy.reactive.server.processor.util.GeneratedClass;

/**
 * Integration point that can add additional
 */
public interface FeatureScanner {

    List<GeneratedClass> integrate(IndexView application, ScannedApplication scannedApplication);

}
