#!/usr/bin/env bash

git clone -b develop --single-branch git@github.com:quarkusio/quarkusio.github.io.git target/web-site

rsync -vr --delete \
    --exclude='**/*.html' \
    --exclude='**/index.adoc' \
    --exclude='**/attributes.adoc' \
    --exclude='**/guides.md' \
    src/main/asciidoc/* \
    target/web-site/_guides

rsync -vr --delete \
    --exclude='**/*.html' \
    --exclude='**/index.adoc' \
    --exclude='**/attributes.adoc' \
    ../target/asciidoc/generated/ \
    target/web-site/_generated-config/latest

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

