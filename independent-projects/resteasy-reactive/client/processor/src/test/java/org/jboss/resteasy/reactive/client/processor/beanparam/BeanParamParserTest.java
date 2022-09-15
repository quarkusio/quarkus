package org.jboss.resteasy.reactive.client.processor.beanparam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import java.io.IOException;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link BeanParamParser}.
 */
public class BeanParamParserTest {

    @Test
    public void mustRecursivelyParseAllParamTypes() throws IOException {
        Index index = Index.of(BeanExample.class, BeanExample.InnerBean.class);
        List<Item> parseResult = BeanParamParser.parse(index.getClassByName(DotName.createSimple(BeanExample.class.getName())),
                index);
        assertNotNull(parseResult);
        parseResult.sort(Comparator.comparing(Item::type));

        assertThat(parseResult).hasSize(4);
        Iterator<Item> itemIterator = parseResult.iterator();

        assertThatNextItemSatisfies(itemIterator, BeanParamItem.class, item -> {
            List<Item> beanParamItems = item.items();
            beanParamItems.sort(Comparator.comparing(Item::type));
            assertThat(beanParamItems).hasSize(2);
            Iterator<Item> subItemIterator = beanParamItems.iterator();
            assertThatNextItemSatisfies(subItemIterator, QueryParamItem.class,
                    subItem -> assertThat(subItem.name()).isEqualTo("queryParam"));
            assertThatNextItemSatisfies(subItemIterator, HeaderParamItem.class,
                    subItem -> assertThat(subItem.getHeaderName()).isEqualTo("headerParam"));
        });

        assertThatNextItemSatisfies(itemIterator, CookieParamItem.class,
                subItem -> assertThat(subItem.getCookieName()).isEqualTo("cookieParam"));
        assertThatNextItemSatisfies(itemIterator, PathParamItem.class,
                subItem -> assertThat(subItem.getPathParamName()).isEqualTo("pathParam"));
        assertThatNextItemSatisfies(itemIterator, FormParamItem.class,
                subItem -> assertThat(subItem.getFormParamName()).isEqualTo("formParam"));
    }

    @Test
    public void mustDetectCycleInBeanParamsChain() throws IOException {
        Index index = Index.of(CyclingBeanParamsExample.class, CyclingBeanParamsExample.InnerBean.class);
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> BeanParamParser.parse(
                        index.getClassByName(DotName.createSimple(CyclingBeanParamsExample.class.getName())),
                        index));
        assertThat(thrown.getMessage()).isEqualTo(
                "Cycle detected in BeanParam annotations; already processed class " + CyclingBeanParamsExample.class.getName());
    }

    @SuppressWarnings("unchecked")
    private <T extends Item> void assertThatNextItemSatisfies(Iterator<Item> itemIterator, Class<T> clazz,
            Consumer<T> condition) {
        Item nextItem = itemIterator.next();
        assertThat(nextItem).isInstanceOf(clazz);
        condition.accept((T) nextItem);
    }

    private static class BeanExample {
        @FormParam("formParam")
        String formParam;

        @CookieParam("cookieParam")
        String cookieParam;

        @PathParam("pathParam")
        String pathParam;

        @BeanParam
        InnerBean innerBean;

        private static class InnerBean {
            @QueryParam("queryParam")
            String queryParam;

            @HeaderParam("headerParam")
            String headerParam;
        }
    }

    private static class CyclingBeanParamsExample {
        @BeanParam
        InnerBean inner;

        private static class InnerBean {
            @BeanParam
            CyclingBeanParamsExample outer;
        }
    }
}
