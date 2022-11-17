#!/usr/bin/env bash

# Make sure our working directory is where this script lives (docs directory of quarkus)
SCRIPTDIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
cd "${SCRIPTDIR}"

# Invocation for sync + publish for a release
# This will clone the website and then sync documents to the 'latest' for a release
#
# $ QUARKUS_WEB_SITE_PUSH=true QUARKUS_RELEASE=true ./sync-web-site.sh ${BRANCH}
#
# In quarkusio/quarkusio.github.io repo for nightly sync (.github/workflows/sync-main-doc.yml)
# Both the branch and the target website directory are specified.
# This will skip the website repo clone and will sync files into _versions, _generated_config, etc.
#
# $ .quarkus-main-repository/docs/sync-web-site.sh main ${PWD}
#
# For local development leave all arguments out.
# This will clone the website repo clone and sync files into _versions, _generated_config, etc.
# (use the version dropdown to select the nightly snapshot to preview changes)
#
# $ ./sync-web-site.sh

if [ $# -eq 0 ]; then
  BRANCH="main"
else
  BRANCH=$1
fi
if [ $# -ge 2 ]; then
  TARGET_DIR=$2
fi

if [ -z $TARGET_DIR ]; then
  TARGET_DIR=target/web-site
  git clone -b develop --single-branch git@github.com:quarkusio/quarkusio.github.io.git ${TARGET_DIR}
fi

if [ $BRANCH == "main" ] && [ "$QUARKUS_RELEASE" == "true" ]; then
  TARGET_GUIDES=${TARGET_DIR}/_guides
  TARGET_CONFIG=${TARGET_DIR}/_generated-doc/latest
  TARGET_INDEX=${TARGET_DIR}/_data/versioned/latest/index
else
  TARGET_GUIDES=${TARGET_DIR}/_versions/${BRANCH}/guides
  TARGET_CONFIG=${TARGET_DIR}/_generated-doc/${BRANCH}
  TARGET_INDEX=${TARGET_DIR}/_data/versioned/${BRANCH//[.]/-}/index
  mkdir -p ${TARGET_GUIDES}
  mkdir -p ${TARGET_INDEX}
fi


echo
echo "Copying from target/asciidoc/sources/* to $TARGET_GUIDES"
echo
rsync -vr --delete \
    --exclude='**/*.html' \
    --exclude='**/index.adoc' \
    --exclude='**/_attributes-local.adoc' \
    --exclude='**/guides.md' \
    --exclude='**/_templates' \
    target/asciidoc/sources/ \
    $TARGET_GUIDES

echo
echo "Copying from ../target/asciidoc/generated/ to $TARGET_CONFIG"
echo
rsync -vr --delete \
    --exclude='**/*.html' \
    --exclude='**/index.adoc' \
    --exclude='**/_attributes.adoc' \
    ../target/asciidoc/generated/ \
    $TARGET_CONFIG

if [ -f target/indexByType.yaml ]; then
  echo
  echo "Copying target/indexByType.yaml to $TARGET_INDEX/quarkus.yaml"
  mkdir -p $TARGET_INDEX
  echo "# Generated file. Do not edit" > $TARGET_INDEX/quarkus.yaml
  cat target/indexByType.yaml >> $TARGET_INDEX/quarkus.yaml
  echo
fi

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

- For either Docker/Podman, you may want to add a volume to store built bundles:
      docker volume create quarkus-jekyll-bundles
  - Add the volume to the command: --volume quarkus-jekyll-bundles:/usr/local/bundle
"
fi
