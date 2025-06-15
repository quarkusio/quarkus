package io.quarkus.cli.deploy;

import java.util.Optional;

import picocli.CommandLine;

public class KubernetesOptions {

    @CommandLine.Option(order = 3, names = { "--api-server-url" }, description = "The URL of the kubernetes API server")
    public Optional<String> masterUrl;

    @CommandLine.Option(order = 4, names = { "--username" }, description = "Kubernetes username")
    public Optional<String> username = Optional.empty();

    @CommandLine.Option(order = 5, names = { "--password" }, description = "Kubernetes password")
    public Optional<String> password = Optional.empty();

    @CommandLine.Option(order = 6, names = { "--token" }, description = "Kubernetes oAuth token")
    public Optional<String> token = Optional.empty();

    @CommandLine.Option(order = 7, names = { "--trust-certs" }, description = "Flag to trust self-signed certificates")
    public Optional<Boolean> trustCerts = Optional.empty();

    @CommandLine.Option(order = 8, names = { "--namespace" }, description = "The Kubernetes namespace")
    public Optional<String> namespace = Optional.empty();

    @CommandLine.Option(order = 9, names = { "--ca-cert-file" }, description = "The CA certificate file")
    public Optional<String> caCertFile = Optional.empty();

    @CommandLine.Option(order = 10, names = { "--ca-cert-data" }, description = "The CA certificate data")
    public Optional<String> caCertData = Optional.empty();

    @CommandLine.Option(order = 11, names = { "--client-cert-file" }, description = "The client certificate file")
    public Optional<String> clientCertFile = Optional.empty();

    @CommandLine.Option(order = 12, names = { "--client-cert-data" }, description = "The client certificate data")
    public Optional<String> clientCertData = Optional.empty();

    @CommandLine.Option(order = 13, names = { "--client-key-file" }, description = "The client key file")
    public Optional<String> clientKeyFile = Optional.empty();

    @CommandLine.Option(order = 14, names = { "--client-key-data" }, description = "The client key data")
    public Optional<String> clientKeyData = Optional.empty();

    @CommandLine.Option(order = 15, names = { "--client-key-algo" }, description = "The client key algorithm")
    public Optional<String> clientKeyAlgo = Optional.empty();

    @CommandLine.Option(order = 16, names = { "--client-key-passphrase" }, description = "The client key passphrase")
    public Optional<String> clientKeyPassphrase = Optional.empty();

    @CommandLine.Option(order = 17, names = {
            "--http-proxy" }, description = "HTTP proxy used to access the Kubernetes API server")
    public Optional<String> httpProxy = Optional.empty();

    @CommandLine.Option(order = 18, names = {
            "--https-proxy" }, description = "HTTPS proxy used to access the Kubernetes API server")
    public Optional<String> httpsProxy = Optional.empty();

    @CommandLine.Option(order = 18, names = { "--proxy-username" }, description = "Proxy username")
    public Optional<String> proxyUsername = Optional.empty();

    @CommandLine.Option(order = 19, names = { "--proxy-password" }, description = "Proxy password")
    public Optional<String> proxyPassword = Optional.empty();

    @CommandLine.Option(order = 19, names = {
            "--no-proxy" }, arity = "0..*", description = "IP addresses or hosts to exclude from proxying")
    public String[] noProxy = new String[0];

    @CommandLine.Option(order = 20, names = {
            "--image-build" }, description = "Perform an image build using the selected builder before deployment")
    public boolean imageBuild;

    @CommandLine.Option(order = 21, names = {
            "--image-builder" }, description = "Perform an image build using the selected builder before deployment")
    public Optional<String> imageBuilder = Optional.empty();

}
