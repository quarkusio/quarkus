#!/usr/bin/env bash

git clone -b develop --single-branch git@github.com:quarkusio/quarkusio.github.io.git target/web-site

if [ $# -eq 0 ]; then
  BRANCH="main"
else
  BRANCH=$1
fi

if [[ $BRANCH == "main" ]]; then
  TARGET_GUIDES=target/web-site/_guides
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
    echo "Run the following command to check the web site (if not done already)"
    echo "(cd target/web-site  && bundle exec jekyll serve)"
fi

