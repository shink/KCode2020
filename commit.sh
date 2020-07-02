#!/bin/bash

set -eu

current_path=/data/kcode2020
path=src/main/java/com.kuaishou.kcode/
target_file=single-thread.java
file=KcodeQuestion.java
times=24
sleep_time=$((RANDOM % 20 + 50))m
message="auto commit by aliyun"

cd $current_path
rm -rf $path$file
cp ../$target_file $path$file

for i in $(seq 1 $times)
do
echo >> $path$file
git add -A
git commit -m "${message}"
git push -u origin master -f
sleep $sleep_time
done
