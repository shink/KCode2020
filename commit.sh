#!/bin/sh

set -eu

apk update && apk upgrade && apk add --no-cache git openssh

mkdir ~/.ssh
echo "${KCODE_KEY}" > ~/.ssh/kcode
chmod 600 ~/.ssh/kcode

mkdir -p /kcode/temp
cp warm-up/src/main/java/com.kuaishou.kcode/KcodeQuestion.java /kcode/temp

git clone ${TARGET_REPO} /kcode/target_repo
cd /kcode/target_repo
git pull
rm -rf src/main/java/com.kuaishou.kcode/KcodeQuestion.java
mv /kcode/temp/KcodeQuestion.java src/main/java/com.kuaishou.kcode

git add -A
git commit -m "auto commit by github actions"
git push
