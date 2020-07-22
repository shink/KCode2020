package com.kuaishou.kcode;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * @author: shenke
 * @project: KCode2020
 * @date: 2020/7/13
 */


public class KcodeAlertAnalysisImpl implements KcodeAlertAnalysis {

    //  线上
//    private final int SERVICE_MAX = 80;
//    private final int IP_MAX = 350;
//    private final int IP_INDEX_MAX = 10;
//    private final int ELAPSE_MAX = 299;
//    private final int TIME_MAX = 60;
//    private final int PATH_COUNT_MAX = 100;
//    private final int PATH_LENGTH_MAX = 30;
//    private final int BUFFER_SIZE = 1 << 18;
//    private final int CACHE_SIZE = 1 << 8;


    //  线下
    private final int SERVICE_MAX = 40;
    private final int IP_MAX = 350;
    private final int IP_INDEX_MAX = 10;
    private final int ELAPSE_MAX = 299;
    private final int TIME_MAX = 11;
    private final int PATH_COUNT_MAX = 30;
    private final int PATH_LENGTH_MAX = 20;
    private final int BUFFER_SIZE = 1 << 18;
    private final int CACHE_SIZE = 1 << 7;

    private char minHourChar;
    private int minMinute;

    private short[] serviceHash;

    private short[] outDegree;
    private short[] inDegree;
    private short[][] graph;
    private short[][] graphIn;
    private boolean[][] flag;
    private short[][][] pathData;
    private short[][][] pathInData;
    private int[] maxCount;
    private int[] maxCountIn;
    private int[] maxLength;
    private int[] maxLengthIn;

    private List<String>[][][][] pathRes;


    /**
     * init
     */
    public KcodeAlertAnalysisImpl() {
        serviceHash = new short[1 << 16];
        pathRes = new List[SERVICE_MAX][SERVICE_MAX][4][TIME_MAX];
        outDegree = new short[SERVICE_MAX];
        inDegree = new short[SERVICE_MAX];
        graph = new short[SERVICE_MAX][SERVICE_MAX];
        graphIn = new short[SERVICE_MAX][SERVICE_MAX];
        flag = new boolean[SERVICE_MAX][SERVICE_MAX];
        pathData = new short[SERVICE_MAX][PATH_COUNT_MAX][PATH_LENGTH_MAX];
        pathInData = new short[SERVICE_MAX][PATH_COUNT_MAX][PATH_LENGTH_MAX];
        maxCount = new int[SERVICE_MAX];
        maxCountIn = new int[SERVICE_MAX];
        maxLength = new int[SERVICE_MAX];
        maxLengthIn = new int[SERVICE_MAX];
    }


    /**
     * @param path       需要分析文件的路径（绝对路径），由评测系统输入
     * @param alertRules 所有报警规则，由评测系统输入
     */
    @Override
    public Collection<String> alarmMonitor(String path, Collection<String> alertRules) throws FileNotFoundException {
        String[] rateArr = new String[10001];
        int idx = 0;
        byte[] bytes = new byte[]{48, 48, 46, 48, 48, 37};
        for (byte i = 48; i < 58; ++i) {
            bytes[0] = i;
            for (byte j = 48; j < 58; ++j) {
                bytes[1] = j;
                for (byte k = 48; k < 58; ++k) {
                    bytes[3] = k;
                    for (byte l = 48; l < 58; ++l) {
                        bytes[4] = l;
                        rateArr[idx++] = new String(bytes);
                    }
                }
            }
        }
        rateArr[0] = ".00%";
        rateArr[idx] = "100.00%";

        InputStream inputStream = new FileInputStream(path);
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        StringBuilder builder = new StringBuilder();

        byte[] readBuffer = new byte[BUFFER_SIZE];
        byte[] cache = new byte[CACHE_SIZE];
        byte[] buffer = new byte[BUFFER_SIZE + CACHE_SIZE];
        int readLength, cacheLength = 0;

        short serviceCount = 0;
        String[] service = new String[SERVICE_MAX];

        short ipCount = 0;
        short[][][] ipHash = new short[256][256][256];
        short[] ipIdxHash = new short[IP_MAX];
        short[][] ipHashIdxMap = new short[SERVICE_MAX][IP_INDEX_MAX];
        short[] hasIpCount = new short[SERVICE_MAX];
        String[] ip = new String[IP_MAX];

        int[] timeCacheIdxArr = new int[]{-1, -1};
        int[][][][] ipData = new int[2][IP_MAX][IP_MAX][ELAPSE_MAX];
        int[][][][] ipRecordData = new int[2][IP_MAX][IP_MAX][2];
        int[][][][] serviceData = new int[2][SERVICE_MAX][SERVICE_MAX][ELAPSE_MAX];
        int[][][][] serviceRecordData = new int[2][SERVICE_MAX][SERVICE_MAX][2];

        int[][][][] ipPairRes = new int[IP_MAX][IP_MAX][TIME_MAX][2];
        for (int i = 1; i < IP_MAX; ++i) {
            for (int j = 1; j < IP_MAX; ++j) {
                for (int k = 0; k < TIME_MAX; ++k) {
                    Arrays.fill(ipPairRes[i][j][k], -1);
                }
            }
        }
        int[][][][] servicePairRes = new int[SERVICE_MAX][SERVICE_MAX][TIME_MAX][2];
        for (int i = 0; i < SERVICE_MAX; ++i) {
            for (int j = 0; j < SERVICE_MAX; ++j) {
                for (int k = 0; k < TIME_MAX; ++k) {
                    Arrays.fill(servicePairRes[i][j][k], -1);
                }
            }
        }

        String callerKey, responderKey, minDate = "";
        short callerIdx, responderIdx, callerIpIdx, responderIpIdx, callerIpHashIdx, responderIpHashIdx;
        int callerHashIdx, responderHashIdx;
        int elapsedTime, timeIdx, curTimeIdx, timeCacheIdx, maxTimeIdx = 0, minHour = 0, hour, minute, stamp, stampDelta, minStamp = 0;
        boolean isSuccess, isFirstLine = true;
        int ipRecords, trueIpRecords, serviceRecords, trueServiceRecords, rate, p99Idx, p99;

        try {
            while ((readLength = inputStream.read(readBuffer)) > 0) {
                //  写入 buffer
                int loc = readLength - 1, end = cacheLength;
                while (readBuffer[loc] != 10) {
                    --loc;
                }
                System.arraycopy(cache, 0, buffer, 0, cacheLength);
                System.arraycopy(readBuffer, 0, buffer, cacheLength, ++loc);
                System.arraycopy(readBuffer, loc, cache, 0, (cacheLength = readLength - loc));
                end += loc;

                //  处理 buffer
                int index = 0;
                while (index < end) {
                    //  解析 caller
                    builder.setLength(0);
                    while (buffer[index] != 44) {
                        builder.append((char) buffer[index++]);
                    }
                    callerKey = builder.toString();
                    callerHashIdx = callerKey.hashCode() & 0xFFFF;
                    callerIdx = serviceHash[callerHashIdx];
                    if (callerIdx == (short) 0) {
                        callerIdx = (serviceHash[callerHashIdx] = ++serviceCount);
                        service[callerIdx] = callerKey;
                    }

                    //  解析 callerIp
                    index += 4;
                    int idx1 = 0, idx2 = 0, idx3 = 0;
                    while (buffer[index] != 46) {
                        idx1 = idx1 * 10 + (buffer[index++] - 48);
                    }
                    ++index;
                    while (buffer[index] != 46) {
                        idx2 = idx2 * 10 + (buffer[index++] - 48);
                    }
                    ++index;
                    while (buffer[index] != 44) {
                        idx3 = idx3 * 10 + (buffer[index++] - 48);
                    }
                    callerIpHashIdx = ipHash[idx1][idx2][idx3];
                    if (callerIpHashIdx == (short) 0) {
                        ipHash[idx1][idx2][idx3] = (callerIpHashIdx = ++ipCount);
                        ipIdxHash[callerIpHashIdx] = (callerIpIdx = ++hasIpCount[callerIdx]);
                        ipHashIdxMap[callerIdx][callerIpIdx] = callerIpHashIdx;
                        builder.setLength(0);
                        builder.append("10.").append(idx1).append(".").append(idx2).append(".").append(idx3);
                        ip[callerIpHashIdx] = builder.toString();
                    }

                    //  解析 responder
                    ++index;
                    builder.setLength(0);
                    while (buffer[index] != 44) {
                        builder.append((char) buffer[index++]);
                    }
                    responderKey = builder.toString();
                    responderHashIdx = responderKey.hashCode() & 0xFFFF;
                    responderIdx = serviceHash[responderHashIdx];
                    if (responderIdx == (short) 0) {
                        responderIdx = (serviceHash[responderHashIdx] = ++serviceCount);
                        service[responderIdx] = responderKey;
                    }

                    //  解析 responderIp
                    index += 4;
                    idx1 = idx2 = idx3 = 0;
                    while (buffer[index] != 46) {
                        idx1 = idx1 * 10 + (buffer[index++] - 48);
                    }
                    ++index;
                    while (buffer[index] != 46) {
                        idx2 = idx2 * 10 + (buffer[index++] - 48);
                    }
                    ++index;
                    while (buffer[index] != 44) {
                        idx3 = idx3 * 10 + (buffer[index++] - 48);
                    }
                    responderIpHashIdx = ipHash[idx1][idx2][idx3];
                    if (responderIpHashIdx == (short) 0) {
                        ipHash[idx1][idx2][idx3] = (responderIpHashIdx = ++ipCount);
                        ipIdxHash[responderIpHashIdx] = (responderIpIdx = ++hasIpCount[responderIdx]);
                        ipHashIdxMap[responderIdx][responderIpIdx] = responderIpHashIdx;
                        builder.setLength(0);
                        builder.append("10.").append(idx1).append(".").append(idx2).append(".").append(idx3);
                        ip[responderIpHashIdx] = builder.toString();
                    }

                    //  是否调用成功
                    isSuccess = buffer[++index] == 116;
                    index += (isSuccess ? 5 : 6);

                    //  计算耗时
                    elapsedTime = 0;
                    while (buffer[index] != 44) {
                        elapsedTime = elapsedTime * 10 + (buffer[index++] - 48);
                    }

                    // 计算时间
                    ++index;
                    stamp = 0;
                    for (int i = 0; i < 10; ++i) {
                        stamp = stamp * 10 + (buffer[index++] - 48);
                    }
                    if (isFirstLine) {
                        minDate = df.format(((long) stamp) * 1000);
                        minHour = Integer.parseInt(minDate.substring(11, 13));
                        minHourChar = minDate.charAt(12);
                        minMinute = Integer.parseInt(minDate.substring(14, 16));
                        stampDelta = Integer.parseInt(minDate.substring(17));
                        minDate = minDate.substring(0, 11);
                        minStamp = stamp - stampDelta;
                        timeIdx = 0;
                        isFirstLine = false;
                    } else {
                        timeIdx = (stamp - minStamp) / 60;
                    }
                    index += 4;

                    //  处理
                    timeCacheIdx = timeIdx % 2;
                    curTimeIdx = timeCacheIdxArr[timeCacheIdx];
                    if (curTimeIdx == -1) {
                        timeCacheIdxArr[timeCacheIdx] = timeIdx;
                    } else if (curTimeIdx != timeIdx) {
                        //  处理数据中的第一分钟
                        for (short i = 1; i <= ipCount; ++i) {
                            for (short j = 1; j <= ipCount; ++j) {
                                if (ipRecordData[timeCacheIdx][i][j][0] == 0)
                                    continue;

                                ipRecords = ipRecordData[timeCacheIdx][i][j][0];
                                trueIpRecords = ipRecordData[timeCacheIdx][i][j][1];
                                rate = (trueIpRecords * 10000) / ipRecords;

                                double d = ipRecords * 0.99;
                                p99Idx = (int) d;
                                if (p99Idx == d)
                                    --p99Idx;
                                for (p99 = ELAPSE_MAX - 1; p99 >= 0; --p99) {
                                    ipRecords -= ipData[timeCacheIdx][i][j][p99];
                                    if (p99Idx >= ipRecords)
                                        break;
                                }

                                ipPairRes[i][j][curTimeIdx][0] = p99;
                                ipPairRes[i][j][curTimeIdx][1] = rate;
                                Arrays.fill(ipData[timeCacheIdx][i][j], (short) 0);
                                ipRecordData[timeCacheIdx][i][j][0] = ipRecordData[timeCacheIdx][i][j][1] = 0;
                            }
                        }

                        for (short i = 1; i <= serviceCount; ++i) {
                            for (short j = 1; j <= serviceCount; ++j) {
                                if (serviceRecordData[timeCacheIdx][i][j][0] == 0)
                                    continue;

                                serviceRecords = serviceRecordData[timeCacheIdx][i][j][0];
                                trueServiceRecords = serviceRecordData[timeCacheIdx][i][j][1];
                                rate = (int) (((long) trueServiceRecords * 10000) / (long) serviceRecords);

                                double d = serviceRecords * 0.99;
                                p99Idx = (int) d;
                                if (p99Idx == d)
                                    --p99Idx;
                                for (p99 = ELAPSE_MAX - 1; p99 >= 0; --p99) {
                                    serviceRecords -= serviceData[timeCacheIdx][i][j][p99];
                                    if (p99Idx >= serviceRecords)
                                        break;
                                }

                                if (!flag[i][j]) {
                                    graph[i][outDegree[i]++] = j;
                                    graphIn[j][inDegree[j]++] = i;
                                    flag[i][j] = true;
                                }
                                servicePairRes[i][j][curTimeIdx][0] = p99;
                                servicePairRes[i][j][curTimeIdx][1] = rate;
                                Arrays.fill(serviceData[timeCacheIdx][i][j], (short) 0);
                                serviceRecordData[timeCacheIdx][i][j][0] = serviceRecordData[timeCacheIdx][i][j][1] = 0;
                            }
                        }

                        timeCacheIdxArr[timeCacheIdx] = timeIdx;
                    }

                    //  添加数据
                    ++serviceData[timeCacheIdx][callerIdx][responderIdx][elapsedTime];
                    ++serviceRecordData[timeCacheIdx][callerIdx][responderIdx][0];
                    ++ipData[timeCacheIdx][callerIpHashIdx][responderIpHashIdx][elapsedTime];
                    ++ipRecordData[timeCacheIdx][callerIpHashIdx][responderIpHashIdx][0];
                    if (isSuccess) {
                        ++serviceRecordData[timeCacheIdx][callerIdx][responderIdx][1];
                        ++ipRecordData[timeCacheIdx][callerIpHashIdx][responderIpHashIdx][1];
                    }
                }
            }

            //  处理最后两分钟
            for (int cid = 0; cid < 2; ++cid) {
                timeCacheIdx = cid;
                curTimeIdx = timeCacheIdxArr[timeCacheIdx];
                maxTimeIdx = Math.max(maxTimeIdx, curTimeIdx);

                for (short i = 1; i <= ipCount; ++i) {
                    for (short j = 1; j <= ipCount; ++j) {
                        if (ipRecordData[timeCacheIdx][i][j][0] == 0)
                            continue;

                        ipRecords = ipRecordData[timeCacheIdx][i][j][0];
                        trueIpRecords = ipRecordData[timeCacheIdx][i][j][1];
                        rate = (trueIpRecords * 10000) / ipRecords;

                        double d = ipRecords * 0.99;
                        p99Idx = (int) d;
                        if (p99Idx == d)
                            --p99Idx;
                        for (p99 = ELAPSE_MAX - 1; p99 >= 0; --p99) {
                            ipRecords -= ipData[timeCacheIdx][i][j][p99];
                            if (p99Idx >= ipRecords)
                                break;
                        }

                        ipPairRes[i][j][curTimeIdx][0] = p99;
                        ipPairRes[i][j][curTimeIdx][1] = rate;
                    }
                }

                for (short i = 1; i <= serviceCount; ++i) {
                    for (short j = 1; j <= serviceCount; ++j) {
                        if (serviceRecordData[timeCacheIdx][i][j][0] == 0)
                            continue;

                        serviceRecords = serviceRecordData[timeCacheIdx][i][j][0];
                        trueServiceRecords = serviceRecordData[timeCacheIdx][i][j][1];
                        rate = (int) (((long) trueServiceRecords * 10000) / (long) serviceRecords);

                        double d = serviceRecords * 0.99;
                        p99Idx = (int) d;
                        if (p99Idx == d)
                            --p99Idx;
                        for (p99 = ELAPSE_MAX - 1; p99 >= 0; --p99) {
                            serviceRecords -= serviceData[timeCacheIdx][i][j][p99];
                            if (p99Idx >= serviceRecords)
                                break;
                        }

                        if (!flag[i][j]) {
                            graph[i][outDegree[i]++] = j;
                            graphIn[j][inDegree[j]++] = i;
                            flag[i][j] = true;
                        }
                        servicePairRes[i][j][curTimeIdx][0] = p99;
                        servicePairRes[i][j][curTimeIdx][1] = rate;
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        //  遍历规则，计算结果
        List<String> res = new ArrayList<>();

        int ruleId, type, condition, threshold;
        int callerIdxStart, callerIdxEnd, responderIdxStart, responderIdxEnd;
        int start, thresholdValue;
        String thresholdValueStr;
        boolean isMeet;

        for (String str : alertRules) {
            int index = 0;
            //  解析 ruleId
            ruleId = 0;
            while (str.charAt(index) != 44) {
                ruleId = ruleId * 10 + (str.charAt(index++) - 48);
            }

            //  解析 caller
            ++index;
            builder.setLength(0);
            while (str.charAt(index) != 44) {
                builder.append(str.charAt(index++));
            }
            callerKey = builder.toString();
            if (callerKey.equals("ALL")) {
                callerIdxStart = 1;
                callerIdxEnd = serviceCount;
            } else {
                callerIdxStart = callerIdxEnd = serviceHash[callerKey.hashCode() & 0xFFFF];
            }

            //  解析 responder
            ++index;
            builder.setLength(0);
            while (str.charAt(index) != 44) {
                builder.append(str.charAt(index++));
            }
            responderKey = builder.toString();
            if (responderKey.equals("ALL")) {
                responderIdxStart = 1;
                responderIdxEnd = serviceCount;
            } else {
                responderIdxStart = responderIdxEnd = serviceHash[responderKey.hashCode() & 0xFFFF];
            }

            //  分别解析两种类型
            if (str.charAt(++index) == 'P') {
                type = 0;
                index += 4;
                condition = 0;
                while (str.charAt(index) != '>') {
                    condition = condition * 10 + (str.charAt(index++) - 48);
                }

                index += 2;
                threshold = 0;
                while (str.charAt(index) != 'm') {
                    threshold = threshold * 10 + (str.charAt(index++) - 48);
                }
            } else {
                type = 1;
                index += 3;
                condition = 0;
                while (str.charAt(index) != '<') {
                    condition = condition * 10 + (str.charAt(index++) - 48);
                }
                index += 2;
                threshold = 0;
                while (str.charAt(index) != '.' && str.charAt(index) != '%') {
                    threshold = threshold * 10 + (str.charAt(index++) - 48);
                }
                threshold *= 100;
                if (str.charAt(index) == '.') {
                    threshold += (str.charAt(++index) - 48) * 10;
                    if (str.charAt(++index) != '%')
                        threshold += (str.charAt(index) - 48);
                }
            }

            //  处理
            for (int i = callerIdxStart; i <= callerIdxEnd; ++i) {
                for (int j = responderIdxStart; j <= responderIdxEnd; ++j) {
                    if (!flag[i][j])
                        continue;

                    for (int k = 1; k <= hasIpCount[i]; ++k) {
                        callerIpHashIdx = ipHashIdxMap[i][k];
                        for (int l = 1; l <= hasIpCount[j]; ++l) {
                            responderIpHashIdx = ipHashIdxMap[j][l];

                            start = 0;
                            for (int m = 0; m <= maxTimeIdx; ++m) {
                                thresholdValue = ipPairRes[callerIpHashIdx][responderIpHashIdx][m][type];
                                if (thresholdValue == -1) {
                                    start = m + 1;
                                    continue;
                                }

                                if (type == 0) {
                                    isMeet = thresholdValue > threshold;
                                    thresholdValueStr = thresholdValue + "ms";
                                } else {
                                    isMeet = thresholdValue < threshold;
                                    thresholdValueStr = rateArr[thresholdValue];
                                }

                                if (isMeet) {
                                    if (m == start + condition - 1) {
                                        //  当前分钟是条件中连续发生分钟的最后一分钟
                                        builder.setLength(0);
                                        hour = minHour + (minMinute + m) / 60;
                                        minute = (minMinute + m) % 60;
                                        builder.append(ruleId).append(',').append(minDate).append(hour < 10 ? "0" + hour : hour).append(':').append(minute < 10 ? "0" + minute : minute).append(',').append(service[i]).append(',').append(ip[callerIpHashIdx]).append(',').append(service[j]).append(',').append(ip[responderIpHashIdx]).append(',').append(thresholdValueStr);
                                        res.add(builder.toString());
                                        ++start;
                                    }
                                } else {
                                    start = m + 1;
                                }
                            }

                        }
                    }
                }
            }
        }

        // ****** 第二问 *****

        //  计算以任意一点为起点的正向、反向最长路径集合
        for (short i = 1; i <= serviceCount; ++i) {
            //  反向
            maxCountIn[i] = 1;
            for (short j = 0; j < inDegree[i]; ++j) {
                dfsIn(i, graphIn[i][j], 1, new short[PATH_LENGTH_MAX]);
            }

            //  正向
            maxCount[i] = 1;
            for (short j = 0; j < outDegree[i]; ++j) {
                dfs(i, graph[i][j], 1, new short[PATH_LENGTH_MAX]);
            }
        }

        //  计算任意两点的最长路径集合，加上时间维度，计算答案
        int pathCacheLen;
        short[] pathCache = new short[PATH_LENGTH_MAX << 3];

        StringBuilder builder3 = new StringBuilder();
        StringBuilder builder2 = new StringBuilder();
        for (short i = 1; i <= serviceCount; ++i) {
            for (short j = 1; j <= serviceCount; ++j) {
                if (!flag[i][j])
                    continue;

                pathCacheLen = maxLengthIn[i] + maxLength[j] + 2;
                for (int k = 0; k < maxCountIn[i]; ++k) {
                    System.arraycopy(pathInData[i][k], 0, pathCache, 0, maxLengthIn[i]);
                    pathCache[maxLengthIn[i]] = i;
                    pathCache[maxLengthIn[i] + 1] = j;
                    for (int l = 0; l < maxCount[j]; ++l) {
                        System.arraycopy(pathData[j][l], 0, pathCache, maxLengthIn[i] + 2, maxLength[j]);

                        builder.setLength(0);
                        for (int m = 0; m < pathCacheLen; ++m) {
                            builder.append(service[pathCache[m]]).append(m == pathCacheLen - 1 ? '|' : "->");
                        }

                        for (int m = 0; m <= maxTimeIdx; ++m) {
                            if (pathRes[i][j][2][m] == null)
                                pathRes[i][j][2][m] = new ArrayList<>();
                            if (pathRes[i][j][3][m] == null)
                                pathRes[i][j][3][m] = new ArrayList<>();
                            builder2.setLength(0);
                            builder3.setLength(0);
                            for (int n = 0; n < pathCacheLen - 2; ++n) {
                                builder3.append(servicePairRes[pathCache[n]][pathCache[n + 1]][m][0]).append("ms,");
                                builder2.append(servicePairRes[pathCache[n]][pathCache[n + 1]][m][1] == -1 ? "-1%" : rateArr[servicePairRes[pathCache[n]][pathCache[n + 1]][m][1]]).append(",");
                            }
                            builder3.append(servicePairRes[pathCache[pathCacheLen - 2]][pathCache[pathCacheLen - 1]][m][0]).append("ms");
                            builder2.append(servicePairRes[pathCache[pathCacheLen - 2]][pathCache[pathCacheLen - 1]][m][1] == -1 ? "-1%" : rateArr[servicePairRes[pathCache[pathCacheLen - 2]][pathCache[pathCacheLen - 1]][m][1]]);
                            pathRes[i][j][2][m].add(builder.toString() + builder2.toString());
                            pathRes[i][j][3][m].add(builder.toString() + builder3.toString());
                        }

                    }
                }
            }
        }

        return res;
    }


    /**
     * 正向 DFS
     */
    private void dfs(short head, short cur, int depth, short[] path) {
        path[depth - 1] = cur;

        if (outDegree[cur] == (short) 0) {
            if (depth == maxLength[head]) {
                System.arraycopy(path, 0, pathData[head][maxCount[head]++], 0, depth);
            } else if (depth > maxLength[head]) {
                System.arraycopy(path, 0, pathData[head][0], 0, depth);
                maxCount[head] = 1;
                maxLength[head] = depth;
            }
        } else {
            for (short i = 0; i < outDegree[cur]; ++i) {
                dfs(head, graph[cur][i], depth + 1, path);
            }
        }
    }


    /**
     * 反向 DFS
     */
    private void dfsIn(short head, short cur, int depth, short[] path) {
        path[PATH_LENGTH_MAX - depth] = cur;

        if (inDegree[cur] == (short) 0) {
            if (depth == maxLengthIn[head]) {
                System.arraycopy(path, PATH_LENGTH_MAX - depth, pathInData[head][maxCountIn[head]++], 0, depth);
            } else if (depth > maxLengthIn[head]) {
                System.arraycopy(path, PATH_LENGTH_MAX - depth, pathInData[head][0], 0, depth);
                maxCountIn[head] = 1;
                maxLengthIn[head] = depth;
            }
        } else {
            for (short i = 0; i < inDegree[cur]; ++i) {
                dfsIn(head, graphIn[cur][i], depth + 1, path);
            }
        }
    }


    /**
     * @param caller    主调服务名称
     * @param responder 被调服务名称
     * @param time      报警发生时间（分钟），格式 yyyy-MM-dd hh:mm
     * @param type      P99 or SR
     */
    @Override
    public Collection<String> getLongestPath(String caller, String responder, String time, String type) {
        return pathRes[serviceHash[caller.hashCode() & 0xFFFF]][serviceHash[responder.hashCode() & 0xFFFF]][type.length()][time.charAt(12) == minHourChar ? (time.charAt(14) - 48) * 10 + (time.charAt(15) - 48) - minMinute : (time.charAt(14) - 48) * 10 + (time.charAt(15) - 48) + 60 - minMinute];
    }
}
