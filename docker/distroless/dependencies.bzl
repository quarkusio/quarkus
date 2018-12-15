
load("@bazel_tools//tools/build_defs/repo:git.bzl", "git_repository", "new_git_repository")
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

def distroless_bazel_repositories():

    git_repository(
        name = "io_bazel_rules_go",
        remote = "https://github.com/bazelbuild/rules_go.git",
        commit = "0.16.5",
    )

    http_archive(
        name = "io_bazel_rules_docker",
        url = "https://github.com/bazelbuild/rules_docker/archive/v0.5.1.zip",
        strip_prefix = "rules_docker-0.5.1",
    )

    git_repository(
        name = "distroless",
        commit = "3585653b2b0d33c3fb369b907ef68df8344fd2ad",
        remote = "https://github.com/GoogleContainerTools/distroless.git",
    )
