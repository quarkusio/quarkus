package io.quarkus.smallrye.reactivemessaging.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import io.smallrye.reactive.messaging.MethodParameterDescriptor;

/**
 * A Quarkus specific implementation of the {@link MethodParameterDescriptor} class. It uses data discovered at
 * build-time to provide the data required at runtime.
 */
public class QuarkusParameterDescriptor implements MethodParameterDescriptor {

    private List<TypeInfo> infos;

    public QuarkusParameterDescriptor() {
        // Empty constructor, required for the proxies
    }

    public QuarkusParameterDescriptor(List<TypeInfo> infos) {
        this.infos = infos;
    }

    public List<TypeInfo> getInfos() {
        return infos;
    }

    public void setInfos(List<TypeInfo> infos) {
        this.infos = infos;
    }

    @Override
    public List<Class<?>> getTypes() {
        if (infos == null) {
            return new ArrayList<>();
        }
        return infos.stream().map(TypeInfo::getName).collect(Collectors.toList());
    }

    @Override
    public Class<?> getGenericParameterType(int paramIndex, int genericIndex) {
        return infos.get(paramIndex).getGenerics().get(genericIndex);
    }
}
