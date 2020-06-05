#!/bin/sh

set -eu

mkdir ~/.ssh
echo "${KCODE_KEY}" > ~/.ssh/kcode
chmod 600 ~/.ssh/kcode

git clone ${TARGET_REPO} target_repo
cd target_repo
git pull
rm -rf src/main/java/com.kuaishou.kcode/KcodeQuestion.java
mv ../warm-up/src/main/java/com.kuaishou.kcode/KcodeQuestion.java src/main/java/com.kuaishou.kcode

git add -A
git commit -m "auto commit by github actions"
git push

