package org.jboss.resteasy.reactive.client.processor.beanparam;

import java.util.List;
import org.jboss.resteasy.reactive.common.model.MethodParameter;
import org.jboss.resteasy.reactive.common.model.ParameterType;

public class ClientBeanParamInfo extends MethodParameter {

    private final List<Item> items;

    public ClientBeanParamInfo(List<Item> items, String beanParamClass) {
        this.items = items;
        setType(beanParamClass);
        setParameterType(ParameterType.BEAN);
    }

    public List<Item> getItems() {
        return items;
    }
}
