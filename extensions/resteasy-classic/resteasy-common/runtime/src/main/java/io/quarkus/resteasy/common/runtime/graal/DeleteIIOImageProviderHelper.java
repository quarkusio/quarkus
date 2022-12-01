package io.quarkus.resteasy.common.runtime.graal;

import com.oracle.svm.core.annotate.Delete;

@Delete("org.jboss.resteasy.plugins.providers.IIOImageProviderHelper")
final class DeleteIIOImageProviderHelper {
}
