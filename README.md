# KCode2020

## Score

- warm-up : 1170541 (rank 10, 单线程)

- preliminary : 17369.21 (rank , 单线程)

## Tricks

1. 尽量使用数组

2. 尽量不要操作字符串

3. 尽量使用较小字节数据类型

4. `read(buffer)` 比 `readLine()` 快很多，`str.split()` 超级慢

5. 在循环次数比较大的循环体中，尽量避免多余的 if-else，并且 if() 中的判断尽量使用较小字节的数据类型，`if(boolean)` 会比 `if(int >= int)` 稍快一点

6. 设计一种映射策略代替 `Map<String, Integer>`

7. 选择适合数据特征的 sort 策略

8. 将 `.` `,` `\n` 等字符直接写为 ASCII 码会稍快一点，但是抖动比较明显 (不知道为啥，求教 :p)

## License

[MIT](LICENSE)
