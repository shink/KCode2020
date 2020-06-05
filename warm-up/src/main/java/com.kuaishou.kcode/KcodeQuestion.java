package com.kuaishou.kcode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

/**
 * @author: shenke
 * @project: KCode2020
 * @date: 2020/5/28
 */

public class KcodeQuestion {
    private static final int STAMP_MAX = 4500;
    private static final int METHOD_MAX = 70;
    private static final int TIME_MAX = 3500;

    private int minStamp;
    private Map<String, Integer> methodMap;
    private String[][] res;


    /**
     * init
     */
    public KcodeQuestion() {
        methodMap = new HashMap<>();
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
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        int[][] array = new int[METHOD_MAX][TIME_MAX];
        int[] arraySum = new int[METHOD_MAX];
        int[] size = new int[METHOD_MAX];
        boolean[] flag = new boolean[METHOD_MAX];

        String line, methodName;
        int currentStamp = 0, methodIndex = 0, elpasedTime;
        char currentChar = 0, ch;

        try {
            if ((line = reader.readLine()) != null) {
                currentChar = line.charAt(9);
                currentStamp = (int) Long.parseLong(line.substring(0, 10));
                int loc = line.lastIndexOf(',');
                methodName = line.substring(14, loc);
                elpasedTime = (int) Long.parseLong(line.substring(loc + 1));

                minStamp = currentStamp;
                methodMap.put(methodName, methodIndex);
                array[methodIndex][0] = elpasedTime;
                arraySum[methodIndex] = elpasedTime;
                ++size[methodIndex];
                flag[methodIndex++] = true;
            }

            while ((line = reader.readLine()) != null) {
                ch = line.charAt(9);
                int loc = line.lastIndexOf(',');
                methodName = line.substring(14, loc);
                elpasedTime = (int) Long.parseLong(line.substring(loc + 1));

                int methodIdx;
                if (methodMap.containsKey(methodName)) {
                    methodIdx = methodMap.get(methodName);
                } else {
                    methodMap.put(methodName, methodIndex);
                    methodIdx = methodIndex++;
                }

                if (ch == currentChar) {
                    array[methodIdx][size[methodIdx]++] = elpasedTime;
                    arraySum[methodIdx] += elpasedTime;
                    flag[methodIdx] = true;
                } else {
                    for (int i = 0; i < METHOD_MAX; ++i) {
                        if (flag[i]) {
                            int qps = size[i], sum = arraySum[i];
                            int[] arr = Arrays.copyOf(array[i], qps);
                            Arrays.sort(arr);

                            int avg = sum / qps;
                            if (avg * qps != sum)
                                ++avg;

                            StringBuilder builder = new StringBuilder(20);
                            builder.append(qps).append(",").append(arr[getIndex(qps, 0.99)]).append(",").append(arr[getIndex(qps, 0.5)]).append(",").append(avg).append(",").append(arr[qps - 1]);
                            res[currentStamp - minStamp][i] = builder.toString();
                            arraySum[i] = 0;
                            size[i] = 0;
                            flag[i] = false;
                        }
                    }

                    currentChar = ch;
                    ++currentStamp;
                    array[methodIdx][0] = elpasedTime;
                    arraySum[methodIdx] = elpasedTime;
                    ++size[methodIdx];
                    flag[methodIdx] = true;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    /**
     * return result
     *
     * @return QPS, P99, P50, AVG, MAX
     */
    public String getResult(Long timestamp, String methodName) {
        return res[timestamp.intValue() - minStamp][methodMap.get(methodName)];
    }
}

