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
# This will skip the website repo clone and will sync files into content/versions, content/_generated-doc, etc.
#
# $ .quarkus-main-repository/docs/sync-web-site.sh main ${PWD}
#
# For local development leave all arguments out.
# This will clone the website repo clone and sync files into content/versions, content/_generated-doc, etc.
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

if [ -z "$TARGET_DIR" ]; then
  TARGET_DIR=target/web-site
  rm -rf "$TARGET_DIR"
  GIT_OPTIONS=""
  if [[ "$QUARKUS_WEB_SITE_PUSH" != "true" ]]; then
    GIT_OPTIONS="--depth=1"
    # Read-only clone: always use HTTPS (works without SSH keys)
    git clone --single-branch $GIT_OPTIONS https://github.com/quarkusio/quarkusio.github.io.git "$TARGET_DIR" || exit 1
  elif [ -n "${RELEASE_GITHUB_TOKEN}" ]; then
    git clone --single-branch $GIT_OPTIONS https://github.com/quarkusio/quarkusio.github.io.git "$TARGET_DIR" || exit 1
  else
    git clone --single-branch $GIT_OPTIONS git@github.com:quarkusio/quarkusio.github.io.git "$TARGET_DIR" || exit 1
  fi
fi

if [ $BRANCH == "main" ] && [ "$QUARKUS_RELEASE" == "true" ]; then
  TARGET_GUIDES=${TARGET_DIR}/content/guides
  TARGET_GENERATED_DOC=${TARGET_DIR}/content/_generated-doc/latest
  TARGET_INDEX=${TARGET_DIR}/_data/versioned/latest/index
else
  TARGET_GUIDES=${TARGET_DIR}/content/versions/${BRANCH}/guides
  TARGET_GENERATED_DOC=${TARGET_DIR}/content/_generated-doc/${BRANCH}
  TARGET_INDEX=${TARGET_DIR}/_data/versioned/${BRANCH//[.]/-}/index
  mkdir -p ${TARGET_GUIDES}
  mkdir -p ${TARGET_INDEX}

  if [ "$QUARKUS_RELEASE" == "true" ]; then
    if [ ! -f ${TARGET_GUIDES}/_attributes-local.adoc ]; then
      cat <<EOF > ${TARGET_GUIDES}/_attributes-local.adoc
// tag::xref-attributes[]
:doc-examples: ./_examples
:generated-dir: ../../../../_generated-doc/${BRANCH}
:code-examples: {generated-dir}/examples
:imagesdir: ./images
:includes: ./_includes
//
:quickstarts-clone-url: -b ${BRANCH} https://github.com/quarkusio/quarkus-quickstarts.git
:quickstarts-archive-url: https://github.com/quarkusio/quarkus-quickstarts/archive/${BRANCH}.zip
:quickstarts-blob-url: https://github.com/quarkusio/quarkus-quickstarts/blob/${BRANCH}
:quickstarts-tree-url: https://github.com/quarkusio/quarkus-quickstarts/tree/${BRANCH}
// end::xref-attributes[]
EOF
    fi

    if [ ! -f ${TARGET_GUIDES}/guides.md ]; then
      cat <<EOF > ${TARGET_GUIDES}/guides.md
---
layout: documentation
title: Guides
link: /version/${BRANCH}/guides/
---
EOF
    fi

    BRANCH_WITH_DASH=${BRANCH/./-}
    if [ ! -f _data/guides-${BRANCH_WITH_DASH}.yaml ]; then
      echo
      echo "##############################################################################################################"
      echo "#"
      echo "# Make sure to create a _data/guides-${BRANCH_WITH_DASH}.yaml index file with a guide index consistent with the ${BRANCH} branch"
      echo "#"
      echo "##############################################################################################################"
      echo
    fi
  fi
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
echo "Post-processing synced guides for Roq..."
echo

# Move static assets to assets/ subdirectory
echo "Moving static assets to assets/ subdirectory..."
for ext in py sh zip jar tar.gz; do
  find "$TARGET_GUIDES" -maxdepth 1 -name "*.$ext" -exec bash -c '
    mkdir -p "$(dirname "$1")/assets"
    mv "$1" "$(dirname "$1")/assets/"
  ' _ {} \;
done

# Update AsciiDoc links to reference ../assets/
echo "Updating asset links in .adoc files..."
for ext in py sh zip jar tar.gz; do
  find "$TARGET_GUIDES" -maxdepth 1 -name "*.adoc" -exec sed -i "s|link:\([a-zA-Z0-9_-]*\.$ext\)|link:../assets/\1|g" {} \;
done

# Ensure index.html files exist in resource directories
echo "Ensuring index.html exists in resource directories..."
for subdir in images javascript assets; do
  dir="$TARGET_GUIDES/$subdir"
  if [ -d "$dir" ] && [ ! -f "$dir/index.html" ]; then
    rel_path=$(echo "$dir" | sed "s|${TARGET_DIR}/content/||")
    printf '%s\n' '---' "link: /${rel_path}/" '---' '<html><body></body></html>' > "$dir/index.html"
    echo "Created index.html for $subdir/"
  fi
done

# Escape Qute syntax in qute-reference guides
echo "Escaping Qute syntax in qute-reference guides..."
find "$TARGET_GUIDES" -name "*qute-reference.adoc" -exec sed -i 's/|}/|\\}/g; s/{|/\\{|/g' {} \;

echo "Post-processing complete."
echo

if [ -d target/quarkus-generated-doc/ ]; then
  echo
  echo "Copying from target/quarkus-generated-doc/ to $TARGET_GENERATED_DOC"
  echo
  rsync -vr --delete \
      --exclude='**/*.html' \
      --exclude='**/index.adoc' \
      --exclude='**/_attributes.adoc' \
      target/quarkus-generated-doc/ \
      $TARGET_GENERATED_DOC
fi

if [ -f target/indexByType.yaml ]; then
  echo
  echo "Copying target/indexByType.yaml to $TARGET_INDEX/quarkus.yaml"
  mkdir -p $TARGET_INDEX
  echo "# Generated file. Do not edit" > $TARGET_INDEX/quarkus.yaml
  cat target/indexByType.yaml >> $TARGET_INDEX/quarkus.yaml
  echo
fi

if [ -f target/relations.yaml ]; then
  echo
  echo "Copying target/relations.yaml to $TARGET_INDEX/relations.yaml"
  mkdir -p $TARGET_INDEX
  echo "# Generated file. Do not edit" > $TARGET_INDEX/relations.yaml
  cat target/relations.yaml >> $TARGET_INDEX/relations.yaml
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
    git push origin main
    echo "Web Site updated - wait for CI build"
else
    echo "
Run one of the following command to check the web site (if not done already):

- If you have Maven set up locally:

    ./target/web-site/serve-only-latest-guides.sh

    OR to preview all versions:

    (cd target/web-site && mvn quarkus:dev)
"
fi
