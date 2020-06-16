# KCode2020

KCode 快手程序设计大赛

## Warm-up

- `commit.sh` : auto commit script

- `KcodeQuestion.java` : using **single thread** (best score: 1170541)

- `KcodeMain.java` : test file

### tricks

1. **use arrays** to store data instead of Map, ArrayList and so on
2. method names can be stored using the following method, so all method names can be mapped to a number between 0 and 99

> $methodIdx = (length * 10 - minLength) + (lastChar - '0')$
>
> $minLength$ is 5 and $lastChar$ represents the last character

## License

[MIT](LICENSE)
