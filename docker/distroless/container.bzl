load(
    "@io_bazel_rules_docker//container:container.bzl",
    container_repositories = "repositories",
    "container_pull",
)

load(
    "@io_bazel_rules_docker//go:image.bzl",
    image_repositories = "repositories",
)

def container_dependencies():
    container_repositories()
    image_repositories()

    container_pull(
        name = "distroless_base",
        registry = "gcr.io",
        repository = "distroless/base",
    )
    
