#!/usr/bin/env bash -e

if which sassc > /dev/null; then
   echo "sassc already installed"
   exit 0;
fi

export SASS_LIBSASS_PATH=~/libsass
export SASS_SASSC_PATH=~/sassc
export SASS_SPEC_PATH=~/sass-spec

git clone git@github.com:sass/sassc.git
cd sassc
script/bootstrap
make
cp bin/sassc ~/bin/.
