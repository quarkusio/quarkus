package io.quarkus.it.hibernate.search.elasticsearch.aws;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;

@QuarkusTestResource(H2DatabaseTestResource.class)
@QuarkusTestResource(WireMockElasticsearchProxyTestResource.class)
public class TestResources {

}
