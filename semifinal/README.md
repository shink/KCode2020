# Semifinal

## Contest questions

<https://kcode-git.kuaishou.com/kcode/kcodealertanalysis/-/blob/master/README.md>

## Local score

- dataset1:
  - alarmMonitor : 47s
  - getLongestPath : 32600ns
- dataset2:
  - alarmMonitor : 29s
  - getLongestPath : 15200ns
- dataset3:
  - alarmMonitor : 9s
  - getLongestPath : 111200ns

> 配置：Inter i5-7500 3.40GHz, Seagate 1T HDD

## Tricks

1. 一阶段又祭天了，所以应当重点关注第二阶段的每个细节，比如数组降维、使用 `hashcode() + magic` 做 service 映射、time 转 timeIdx 等

2. 可以通过 `serviceHash[service.hashcode() & 0xFFFF] -> [0, 80)` 做 service 映射

3. 可以事先计算好起始时间的小时和分钟，第二问时根据这两个数字计算 timeIdx

4. `P99` 的 `length()` 是 3，`SR` 的 `length()` 是 2，可以通过这个判断是何种类型
