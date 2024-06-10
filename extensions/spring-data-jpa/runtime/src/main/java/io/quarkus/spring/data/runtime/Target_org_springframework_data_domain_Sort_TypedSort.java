package io.quarkus.spring.data.runtime;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import com.oracle.svm.core.annotate.TargetElement;

@TargetClass(className = "org.springframework.data.domain.Sort", innerClass = "TypedSort")
public final class Target_org_springframework_data_domain_Sort_TypedSort {

    @Substitute
    @TargetElement(name = TargetElement.CONSTRUCTOR_NAME)
    public Target_org_springframework_data_domain_Sort_TypedSort(Class<?> type) {

    }

}
