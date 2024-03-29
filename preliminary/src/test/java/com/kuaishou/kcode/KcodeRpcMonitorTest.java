package com.kuaishou.kcode;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static java.lang.System.nanoTime;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.stream.Collectors.toSet;

/**
 * @author kcode
 * Created on 2020-06-01
 * 该评测程序主要便于选手在本地优化和调试自己的程序
 */
public class KcodeRpcMonitorTest {
    public static void main(String[] args) throws Exception {
        KcodeRpcMonitor kcodeRpcMonitor = new KcodeRpcMonitorImpl();

        long startNs = nanoTime();
        kcodeRpcMonitor.prepare("datasets/test.data");
        System.out.println("prepare elapsed time: " + NANOSECONDS.toMillis(nanoTime() - startNs) + "ms");

        // 读取checkPair.result文件
        //        Map<CheckPairKey, Set<CheckPairResult>> checkPairMap = createCheckPairMap("请更换成自己本地的checkPair.result路径 ");
        //
        //        // 读取checkResponder.result文件
        //        Map<CheckResponderKey, CheckResponderResult> checkResponderMap = createCheckResponderMap("请更换成自己本地的checkResponder.result路径");
        //
        //        // 评测checkPair
        //        checkPair(kcodeRpcMonitor, checkPairMap);
        //
        //        // 评测checkResponder
        //        checkResponder(kcodeRpcMonitor, checkResponderMap);

    }

    public static void checkPair(KcodeRpcMonitor kcodeRpcMonitor,
                                 Map<CheckPairKey, Set<CheckPairResult>> checkPairMap) {
        int checkPairTime = 10000; // 可以自己修改次数
        long cast = 0L;
        while (true) {
            for (Map.Entry<CheckPairKey, Set<CheckPairResult>> entry : checkPairMap.entrySet()) {
                CheckPairKey key = entry.getKey();
                long startNs = nanoTime();
                List<String> result = kcodeRpcMonitor.checkPair(key.getCaller(), key.getResponder(), key.getTime());
                cast += (nanoTime() - startNs);
                Set<CheckPairResult> checkResult = entry.getValue();
                if (Objects.isNull(result) || checkResult.size() != result.size()) {
                    System.out.println("key:" + key + ", result:" + result + ", checkResult:" + checkResult);
                    throw new RuntimeException("评测结果错误");
                }
                if (result.size() != 0) {
                    Set<CheckPairResult> checkPairResSet = result.stream().map(CheckPairResult::new).collect(toSet());
                    if (!checkResult.containsAll(checkPairResSet)) {
                        System.out.println("key:" + key + ", result:" + result + ", checkResult:" + checkResult);
                        throw new RuntimeException("评测结果错误");
                    }
                }
                if (checkPairTime-- <= 0) {
                    System.out.println("checkPair 结束, cast(ms):" + NANOSECONDS.toMillis(cast));
                    return;
                }
            }
        }
    }

    public static void checkResponder(KcodeRpcMonitor kcodeRpcMonitor,
                                      Map<CheckResponderKey, CheckResponderResult> checkResponderMap) {
        int checkResponderTime = 1000; // 可以自己修改次数
        long cast = 0L;
        while (true) {
            for (Map.Entry<CheckResponderKey, CheckResponderResult> entry : checkResponderMap.entrySet()) {
                CheckResponderKey key = entry.getKey();
                long startNs = nanoTime();
                String result = kcodeRpcMonitor.checkResponder(key.getName(), key.getStartTime(), key.getEndTime());
                cast += (nanoTime() - startNs);
                CheckResponderResult checkResponderResult = entry.getValue();
                if (Objects.isNull(result) || !checkResponderResult.equals(new CheckResponderResult(result))) {
                    System.out.println("key:" + key + ", result:" + result + ", checkResult:" + checkResponderResult);
                    throw new RuntimeException("评测结果错误");
                }
                if (checkResponderTime-- <= 0) {
                    System.out.println("checkResponder 结束, cast(ms):" + NANOSECONDS.toMillis(cast));
                    return;
                }
            }
        }
    }
}
