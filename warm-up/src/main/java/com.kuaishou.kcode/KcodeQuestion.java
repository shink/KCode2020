package com.kuaishou.kcode;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;


/**
 * @author: shenke
 * @project: KCode2020
 * @date: 2020/5/28
 */


public class KcodeQuestion {
    private static final int STAMP_MAX = 4200;
    private static final int METHOD_MAX = 100;
    private static final int TIME_MAX = 3200;
    private static final int BUFFER_SIZE = 1 << 18;
    private static final int CACHE_SIZE = 1 << 6;

    private int minStamp;
    private String[][] res;


    /**
     * init
     */
    public KcodeQuestion() {
        res = new String[STAMP_MAX][METHOD_MAX];
    }


    /**
     * return n*p%
     */
    private int getIndex(int n, double p) {
        double d = n * p;
        int num = (int) d;
        return num == d ? num - 1 : num;
    }


    /**
     * deal with input data
     */
    public void prepare(InputStream inputStream) {
        byte[] buffer = new byte[BUFFER_SIZE];
        byte[] cache = new byte[CACHE_SIZE];
        int bufferLength, cacheLength = 0;

        int stamp = 0, methodIdx, elpasedTime;
        byte currentByte = 0;

        int[][] array = new int[METHOD_MAX][TIME_MAX];
        int[] arraySum = new int[METHOD_MAX];
        int[] size = new int[METHOD_MAX];
        boolean[] flag = new boolean[METHOD_MAX];

        try {
            //  处理第一块（主要是为了处理第一行）
            if ((bufferLength = inputStream.read(buffer)) > 0) {
                int end = bufferLength - 1;
                while (buffer[end] != 10) {
                    --end;
                }
                cacheLength = bufferLength - ++end;
                System.arraycopy(buffer, end, cache, 0, cacheLength);

                int idx = 0;
                stamp = 0;
                while (idx < 10) {
                    stamp = stamp * 10 + buffer[idx++] - 48;
                }
                minStamp = stamp;
                stamp = 0;
                currentByte = buffer[9];

                idx += 4;
                while (idx < end) {
                    methodIdx = idx;
                    while (buffer[idx] != 44) {
                        ++idx;
                    }
                    methodIdx = (idx - methodIdx - 5) * 10 + (buffer[idx - 1] - 48);

                    ++idx;
                    elpasedTime = 0;
                    while (buffer[idx] != 10) {
                        elpasedTime = elpasedTime * 10 + buffer[idx++] - 48;
                    }

                    array[methodIdx][size[methodIdx]++] = elpasedTime;
                    arraySum[methodIdx] += elpasedTime;
                    flag[methodIdx] = true;
                    idx += 15;
                }
            }

            //  处理剩余块
            while ((bufferLength = inputStream.read(buffer)) > 0) {
                if (bufferLength < BUFFER_SIZE) {
                    buffer = Arrays.copyOf(buffer, bufferLength);
                }

                int idx = 0;
                while (buffer[idx] != 10) {
                    ++idx;
                }
                System.arraycopy(buffer, 0, cache, cacheLength, ++idx);

                //  处理缓存中的一行
                int i = 14;
                methodIdx = i;
                while (cache[i] != 44) {
                    ++i;
                }
                methodIdx = (i - methodIdx - 5) * 10 + (cache[i - 1] - 48);

                ++i;
                elpasedTime = 0;
                while (cache[i] != 10) {
                    elpasedTime = elpasedTime * 10 + cache[i++] - 48;
                }

                array[methodIdx][size[methodIdx]++] = elpasedTime;
                arraySum[methodIdx] += elpasedTime;
                flag[methodIdx] = true;

                //  将 buffer 中多余部分写入 cache
                int end = bufferLength - 1;
                while (buffer[end] != 10) {
                    --end;
                }
                cacheLength = bufferLength - ++end;
                System.arraycopy(buffer, end, cache, 0, cacheLength);

                //  处理 buffer 中剩余部分
                while (idx < end) {
                    if (currentByte != buffer[idx + 9]) {
                        StringBuilder builder = new StringBuilder(20);
                        for (int j = 0; j < METHOD_MAX; ++j) {
                            if (!flag[j])
                                continue;

                            int qps = size[j], sum = arraySum[j];
                            int[] arr = Arrays.copyOf(array[j], qps);
                            Arrays.sort(arr);

                            int avg = sum / qps;
                            if (avg * qps != sum) {
                                ++avg;
                            }

                            builder.setLength(0);
                            builder.append(qps).append(',').append(arr[getIndex(qps, 0.99)]).append(',').append(arr[getIndex(qps, 0.5)]).append(',').append(avg).append(',').append(arr[qps - 1]);
                            res[stamp][j] = builder.toString();
                            arraySum[j] = 0;
                            size[j] = 0;
                            flag[j] = false;
                        }
                        ++stamp;
                        currentByte = buffer[idx + 9];
                    }

                    idx += 14;
                    methodIdx = idx;
                    while (buffer[idx] != 44) {
                        ++idx;
                    }
                    methodIdx = (idx - methodIdx - 5) * 10 + (buffer[idx - 1] - 48);

                    ++idx;
                    elpasedTime = 0;
                    while (buffer[idx] != 10) {
                        elpasedTime = elpasedTime * 10 + buffer[idx++] - 48;
                    }

                    array[methodIdx][size[methodIdx]++] = elpasedTime;
                    arraySum[methodIdx] += elpasedTime;
                    flag[methodIdx] = true;
                    ++idx;
                }
            }

        } catch (
                IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * return result
     *
     * @return QPS, P99, P50, AVG, MAX
     */
    public String getResult(Long timestamp, String methodName) {
        return res[timestamp.intValue() - minStamp][(methodName.length() - 5) * 10 + (methodName.charAt(methodName.length() - 1) - 48)];
    }
}
