
package io.quarkus.cli.image;

import java.util.Optional;

import picocli.CommandLine;

public class ImageOptions {

    @CommandLine.Option(order = 3, names = {
            "--group" }, description = "The group part of the container image. Defaults to the ${user.name}.")
    public Optional<String> group = Optional.empty();

    @CommandLine.Option(order = 4, names = {
            "--name" }, description = "The name part of the container image. Defaults to the ${project.artifactId}.")
    public Optional<String> name = Optional.empty();

    @CommandLine.Option(order = 5, names = {
            "--tag" }, description = "The tag part of the container image. Defaults to the ${project.version}.")
    public Optional<String> tag = Optional.empty();

    @CommandLine.Option(order = 6, names = {
            "--registry" }, description = "The registry part of the container image. Empty by default.")
    public Optional<String> registry = Optional.empty();

    @Override
    public String toString() {
        return "ImageOptions [group=" + group + ", name=" + name + ", tag=" + tag + ", registry=" + registry + "]";
    }
}
