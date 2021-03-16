package io.quarkus.grpc.common.runtime.graal;

import static io.grpc.InternalServiceProviders.getCandidatesViaHardCoded;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@SuppressWarnings("unused")
@TargetClass(className = "io.grpc.ServiceProviders")
final class Target_io_grpc_ServiceProviders { // NOSONAR

    @Substitute
    public static <T> List<T> loadAll(
            Class<T> klass,
            Iterable<Class<?>> hardcoded,
            ClassLoader cl,
            final Target_io_grpc_ServiceProviders_PriorityAccessor<T> priorityAccessor) {

        // For loading classes directly instead of using SPI.

        Iterable<T> candidates;
        candidates = getCandidatesViaHardCoded(klass, hardcoded);
        List<T> list = new ArrayList<>();
        for (T current : candidates) {
            if (!priorityAccessor.isAvailable(current)) {
                continue;
            }
            list.add(current);
        }

        // DO NOT USE LAMBDA
        //noinspection Java8ListSort,Convert2Lambda
        Collections.sort(list, Collections.reverseOrder(new Comparator<T>() { // NOSONAR
            @Override
            public int compare(T f1, T f2) {
                int pd = priorityAccessor.getPriority(f1) - priorityAccessor.getPriority(f2);
                if (pd != 0) {
                    return pd;
                }
                return f1.getClass().getName().compareTo(f2.getClass().getName());
            }
        }));
        return Collections.unmodifiableList(list);
    }
}

@TargetClass(className = "io.grpc.ServiceProviders", innerClass = "PriorityAccessor")
interface Target_io_grpc_ServiceProviders_PriorityAccessor<T> { // NOSONAR
    // Just provide access to "io.grpc.ServiceProviders.PriorityAccessor"

    @Alias
    boolean isAvailable(T provider);

    @Alias
    int getPriority(T provider);
}

@SuppressWarnings("unused")
class GrpcSubstitutions {
}
