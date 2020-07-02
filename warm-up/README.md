# Warm-up

## Contest questions

<https://kcode-git.kuaishou.com/kcode/kcode-warm-up/-/blob/master/README.md>

## Local score

- prepare : 13s

> 配置：Inter i5-6200U 2.30GHz, Plextor 256GB SSD

## Tricks

1. 秒级时间戳是递增的，当判断秒级时间戳是否发生变化时（即需要处理时），只需要依据时间戳的第 10 位进行比较

2. methodName 共有 69 个，观察发现仅需要 methodName.length() 和最后一位数字即可唯一确定，最终可以将 String 类型的 methodName 表示为 [0, 99] 的 int (实际只需要 [0, 81])

3. 方法耗时区间范围较大，不适合 Bucket sort
