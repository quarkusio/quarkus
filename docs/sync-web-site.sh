#!/usr/bin/env bash

git clone -b develop --single-branch git@github.com:quarkusio/quarkusio.github.io.git target/web-site

if [ $# -eq 0 ]; then
  BRANCH="main"
else
  BRANCH=$1
fi

if [[ $BRANCH == "main" ]]; then
  TARGET_GUIDES=target/web-site/_versions/main/guides
  TARGET_CONFIG=target/web-site/_generated-config/latest
else
  TARGET_GUIDES=target/web-site/_versions/${BRANCH}/guides
  TARGET_CONFIG=target/web-site/_generated-config/${BRANCH}
fi

rsync -vr --delete \
    --exclude='**/*.html' \
    --exclude='**/index.adoc' \
    --exclude='**/attributes.adoc' \
    --exclude='**/guides.md' \
    src/main/asciidoc/* \
    $TARGET_GUIDES

rsync -vr --delete \
    --exclude='**/*.html' \
    --exclude='**/index.adoc' \
    --exclude='**/attributes.adoc' \
    ../target/asciidoc/generated/ \
    $TARGET_CONFIG

echo "Sync done!"
echo "=========="

if [[ "$QUARKUS_WEB_SITE_PUSH"  = "true" ]]
then
    echo "Updating the web site"
    cd target/web-site
    git add -A
    git commit -m "Sync web site with Quarkus documentation"
    git push origin develop
    echo "Web Site updated - wait for CI build"
else
    echo "
Run one of the following command to check the web site (if not done already):

- If you have Jekyll set up locally:
    (cd target/web-site && bundle exec jekyll serve)

- If you have Docker or Podman:
    cd target/web-site
    docker run --rm --volume=\"$PWD:/srv/jekyll:Z\" \\
        --publish 4000:4000 jekyll/jekyll:4.1.0 jekyll serve --incremental
  
- If you have Podman, something similar should work, but...
  - you may need to set the Jekyll user/group id to match yours: -e JEKYLL_UID=501 -e JEKYLL_GID=503
  - you may need to add an environment variable if you are running rootless: -e JEKYLL_ROOTLESS=1
  - More: https://github.com/envygeeks/jekyll-docker/blob/master/README.md

- For either docker/podman, you may want to add a volume to store built bundles:
      docker volume create quarkus-jekyll-bundles
  - Add the volume to the command: --volume quarkus-jekyll-bundles:/usr/local/bundle
"
fi
