#!/usr/bin/env bash
git clone git@github.com:quarkusio/quarkusio.github.io.git target/web-site
rsync -vr \
    --exclude='**/*.html' \
    --exclude='**/index.adoc' \
    --exclude='**/attributes.adoc' \
    src/main/asciidoc/* \
    target/web-site/_guides

echo "Sync done!"
echo "=========="

if [[ "$QUARKUS_WEB_SITE_PUSH"  = "true" ]]
then
    echo "Updating the web site"
    cd target/web-site
    git add -A
    git commit -m "sync web site with quarkus documentation"
    git push origin develop
    echo "Web Site updated - wait for CI build"
else
    echo "Run the following command to check the web site (if not done already)"
    echo "cd target/web-site  && bundle exec jekyll serve"
fi


