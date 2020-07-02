# Preliminary

## Contest questions

<https://kcode-git.kuaishou.com/kcode/KcodeRpcMonitor/-/blob/master/README.md>

## Local score

- prepare : 34s
- checkPair : 210ms (100w)
- checkResponder : 88ms (100w)

> 配置：Inter i5-6200U 2.30GHz, Plextor 256GB SSD

## Tricks

1. service 无法做到像热身赛那样直接映射为一个小范围内的 int 值，可以使用一个 `int[][]` 做映射。观察发现每个服务名都是由一个 `service+数字` 组成，并且每个 service 的首字母各不相同，因此可将第一维设为首字母，第二维设为结尾的数字，并且结尾数字可以仅使用从倒数第五位开始的两位数字表示，故映射数组为：int[26][100] -> [1,80] (实际只需要 `int[21][100] -> [1,80]`)

2. ip 共有 450 个，观察发现每个 ip 均为 `10.` 开头的内网地址，并且与服务有着一对多的关系，每个 ip 只对应唯一一个服务，每个服务最多对应 9 个 ip，因此可以先将 ip 映射到 [1, 450] (`int [255][255][255] -> [1, 450]`)，再将 ipIdx 映射到 [1, 9]

3. 方法耗时范围均在 [0, 298] 之间，适合 Bucket sort，寻找 p99 时只需从结尾依次寻找即可

4. 调用成功率可以事先保存为一个 `String[10001]` 的数组，下标为 `成功率 * 100的 int 值`

5. `checkResponder` 阶段需要返回一段时间范围内的被调成功率，因此可以事先穷举所有情况，加快查询速度
