package io.quarkus.azure.app.config.client.runtime;

interface AzureAppConfigClientGateway {

    Response exchange() throws Exception;
}
