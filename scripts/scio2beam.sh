#!/bin/bash

# script to port Scio commits to Beam
#
# Usage example:

# cd ~src/scio
# git checkout apache-beam
# git pull  # ensure branch is fresh
# git merge master  # merge changes from master

# # fix merge conflicts

# # apply commits since origin/apache-beam to ~/src/incubator-beam
# ./scripts/scio2beam.sh ~/src/scio ~/src/incubator-beam origin/apache-beam

if [ $# != 3 ]; then
    echo "Usage: $0 <scio-directory> <beam-directory> [ <since> | <revision range> ]"
    exit 1
fi

scio=$(readlink -f $1)
beam=$(readlink -f $2)
commit=$3
patch="$scio/diff.patch"

cd $scio
git format-patch $commit --stdout | sed -e 's/\<\([ab]\)\/scio-\([a-z]\+\)/\1\/sdks\/scala\/core/g' > $patch

cd $beam
git am --reject $patch

rm $patch
