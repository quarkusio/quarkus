load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

load(
    "@distroless//package_manager:package_manager.bzl",
    "package_manager_repositories",
    "dpkg_src",
    "dpkg_list",
)

def debian_dependencies():

    package_manager_repositories()

    dpkg_src(
        name = "debian_stretch",
        arch = "amd64",
        distro = "stretch",
        sha256 = "4cb2fac3e32292613b92d3162e99eb8a1ed7ce47d1b142852b0de3092b25910c",
        snapshot = "20180406T095535Z",
        url = "http://snapshot.debian.org/archive",
    )

    dpkg_src(
        name = "debian_stretch_backports",
        arch = "amd64",
        distro = "stretch-backports",
        sha256 = "2863af9484d2d6b478ef225a8c740dac9a14015a594241a0872024c873123cdd",
        snapshot = "20180406T095535Z",
        url = "http://snapshot.debian.org/archive",
    )

    dpkg_src(
        name = "debian_stretch_security",
        package_prefix = "http://snapshot.debian.org/archive/debian-security/20180405T165926Z/",
        packages_gz_url = "http://snapshot.debian.org/archive/debian-security/20180405T165926Z/dists/stretch/updates/main/binary-amd64/Packages.gz",
        sha256 = "a503fb4459eb9e862d080c7cf8135d7d395852e51cc7bfddf6c3d6cc4e11ee5f",
    )

    dpkg_list(
        name = "package_bundle",
        packages = [            
            "zlib1g"
        ],        
        sources = [
            "@debian_stretch_security//file:Packages.json",
            "@debian_stretch_backports//file:Packages.json",
            "@debian_stretch//file:Packages.json",
        ],
)
