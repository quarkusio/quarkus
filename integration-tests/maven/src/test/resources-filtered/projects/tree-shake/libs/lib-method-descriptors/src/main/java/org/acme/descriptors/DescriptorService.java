package org.acme.descriptors;

public class DescriptorService {
    public ReturnType process(ParamType param) {
        return new ReturnType("processed:" + param.getValue());
    }
}
