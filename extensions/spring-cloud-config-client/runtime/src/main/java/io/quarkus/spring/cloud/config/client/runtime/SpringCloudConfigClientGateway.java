package io.quarkus.spring.cloud.config.client.runtime;

interface SpringCloudConfigClientGateway {

    Response exchange(String applicationName, String profile) throws Exception;
}
