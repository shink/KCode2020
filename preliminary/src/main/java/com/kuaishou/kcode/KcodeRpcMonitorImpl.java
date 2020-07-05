package com.kuaishou.kcode;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * @author: shenke
 * @project: Kcode2020
 * @date: 2020/6/17
 */


public class KcodeRpcMonitorImpl implements KcodeRpcMonitor {

    private static final int SERVICE_MAX = 81;
    private static final int IP_INDEX_MAX = 10;
    private static final int ELAPSE_MAX = 299;
    private static final int TIME_MAX = 30;

    private static final int TIME_MIN = 33;     // 线下
    private static final int STAMP_MIN = 299980;    //  线下
    private static final int STAMP_LEN = 6;     // 线下
    //    private static final int TIME_MIN = 6;     // 线上
    //    private static final int STAMP_MIN = 360;    //  线上
    //    private static final int STAMP_LEN = 4;     //    线上

    private static final int BUFFER_SIZE = 1 << 18;
    private static final int CACHE_SIZE = 1 << 7;

    private int serviceCount;
    private int[][] serviceHash;
    private List<String>[][][] checkPairRes;
    private String[][][] checkResponderRes;


    /**
     * init
     */
    public KcodeRpcMonitorImpl() {
        serviceCount = 1;
        serviceHash = new int[21][100];
        checkPairRes = new ArrayList[TIME_MAX][SERVICE_MAX][SERVICE_MAX];
        checkResponderRes = new String[SERVICE_MAX][TIME_MAX][TIME_MAX];
    }


    /**
     * @return time's hash index
     */
    private int getTimeIdx(String time) {
        //  线下
        if (time.charAt(9) != 54 || (time.charAt(12) != 55 && time.charAt(12) != 56))
            return -1;

        int timeIdx = (time.charAt(14) - (time.charAt(12) == 55 ? 48 : 42)) * 10;
        timeIdx += (time.charAt(15) - 48 - TIME_MIN);

        //  线上
        //        if (time.charAt(9) != 53 || time.charAt(11) != 49 || time.charAt(12) != 49)
        //            return -1;
        //
        //        int timeIdx = (time.charAt(14) - 48) * 10 + (time.charAt(15) - 48) - TIME_MIN;

        return (timeIdx >= 0 && timeIdx < TIME_MAX) ? timeIdx : -1;
    }


    /**
     * @return service's hash index
     */
    private int getServiceIdx(String service) {
        int idx1 = service.charAt(0) - 97, idx2 = 0;
        int length = service.length();
        int count = 2;

        for (int i = length - 5; i < length; ++i) {
            int num = service.charAt(i);
            if (num > 57)
                continue;
            idx2 = idx2 * 10 + (num - 48);
            if (--count == 0)
                break;
        }

        if (serviceHash[idx1][idx2] == 0) {
            serviceHash[idx1][idx2] = serviceCount;
            return serviceCount++;
        } else {
            return serviceHash[idx1][idx2];
        }
    }


    /**
     * 接收和分析调用信息的接口
     *
     * @param path, 需要分析文件的路径（绝对路径），由评测系统输入
     */
    public void prepare(String path) throws FileNotFoundException {
        String[] rate = new String[10001];
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
                        rate[idx++] = new String(bytes);
                    }
                }
            }
        }
        rate[idx] = "100.00%";

        InputStream inputStream = new FileInputStream(path);
        StringBuilder builder = new StringBuilder();

        byte[] readBuffer = new byte[BUFFER_SIZE];
        byte[] cache = new byte[CACHE_SIZE];
        byte[] buffer = new byte[BUFFER_SIZE + CACHE_SIZE];
        int readLength, cacheLength = 0;

        short[][][] ipHash = new short[255][255][255];
        short[] ipCount = new short[SERVICE_MAX];
        String[][] ip = new String[SERVICE_MAX][IP_INDEX_MAX];

        short[][][][][] pairData = new short[SERVICE_MAX][SERVICE_MAX][IP_INDEX_MAX][IP_INDEX_MAX][ELAPSE_MAX];
        int[][][][][] recordData = new int[SERVICE_MAX][SERVICE_MAX][IP_INDEX_MAX][IP_INDEX_MAX][2];
        int[][] responderData = new int[TIME_MAX][SERVICE_MAX];

        int callerIdx, responderIdx, timeIdx = 0, curTimeIdx = 0;
        short callerIpIdx, responderIpIdx;
        int stamp, elapsedTime;
        int qps, trueQps, rateIdx, p99Idx, p99, records, trueRecords;
        boolean isSuccess;

        try {
            while ((readLength = inputStream.read(readBuffer)) > 0) {
                //  写入 buffer
                int loc = readLength - 1, end = cacheLength;
                while (readBuffer[loc] != 10) {
                    --loc;
                }
                System.arraycopy(cache, 0, buffer, 0, cacheLength);
                System.arraycopy(readBuffer, 0, buffer, cacheLength, ++loc);
                cacheLength = readLength - loc;
                System.arraycopy(readBuffer, loc, cache, 0, cacheLength);
                end += loc;

                //  处理 buffer
                int index = 0;
                while (index < end) {
                    //  计算 callerIdx
                    int idx1 = index, idx2 = 0, count = 2;
                    while (buffer[index] != 44) {
                        ++index;
                    }
                    for (int i = index - 5; i < index; ++i) {
                        int num = buffer[i];
                        if (num > 57)
                            continue;
                        idx2 = idx2 * 10 + (num - 48);
                        if (--count == 0)
                            break;
                    }
                    idx1 = buffer[idx1] - 97;
                    if (serviceHash[idx1][idx2] == 0) {
                        serviceHash[idx1][idx2] = serviceCount++;
                    }
                    callerIdx = serviceHash[idx1][idx2];

                    //  计算 callerIpIdx
                    index += 4;
                    int idx3 = 0, idx4 = 0, idx5 = 0;
                    while (buffer[index] != 46) {
                        idx3 = idx3 * 10 + (buffer[index++] - 48);
                    }
                    ++index;
                    while (buffer[index] != 46) {
                        idx4 = idx4 * 10 + (buffer[index++] - 48);
                    }
                    ++index;
                    while (buffer[index] != 44) {
                        idx5 = idx5 * 10 + (buffer[index++] - 48);
                    }
                    if (ipHash[idx3][idx4][idx5] == 0) {
                        callerIpIdx = ++ipCount[callerIdx];
                        ipHash[idx3][idx4][idx5] = callerIpIdx;
                        builder.setLength(0);
                        builder.append("10.").append(idx3).append(".").append(idx4).append(".").append(idx5);
                        ip[callerIdx][callerIpIdx] = builder.toString();
                    } else {
                        callerIpIdx = ipHash[idx3][idx4][idx5];
                    }

                    //  计算 responderIdx
                    idx1 = ++index;
                    idx2 = 0;
                    count = 2;
                    while (buffer[index] != 44) {
                        ++index;
                    }
                    for (int i = index - 5; i < index; ++i) {
                        int num = buffer[i];
                        if (num > 57)
                            continue;
                        idx2 = idx2 * 10 + (num - 48);
                        if (--count == 0)
                            break;
                    }
                    idx1 = buffer[idx1] - 97;
                    if (serviceHash[idx1][idx2] == 0) {
                        serviceHash[idx1][idx2] = serviceCount++;
                    }
                    responderIdx = serviceHash[idx1][idx2];

                    //  计算 responderIpIdx
                    index += 4;
                    idx3 = 0;
                    idx4 = 0;
                    idx5 = 0;
                    while (buffer[index] != 46) {
                        idx3 = idx3 * 10 + (buffer[index++] - 48);
                    }
                    ++index;
                    while (buffer[index] != 46) {
                        idx4 = idx4 * 10 + (buffer[index++] - 48);
                    }
                    ++index;
                    while (buffer[index] != 44) {
                        idx5 = idx5 * 10 + (buffer[index++] - 48);
                    }
                    if (ipHash[idx3][idx4][idx5] == 0) {
                        responderIpIdx = ++ipCount[responderIdx];
                        ipHash[idx3][idx4][idx5] = responderIpIdx;
                        builder.setLength(0);
                        builder.append("10.").append(idx3).append(".").append(idx4).append(".").append(idx5);
                        ip[responderIdx][responderIpIdx] = builder.toString();
                    } else {
                        responderIpIdx = ipHash[idx3][idx4][idx5];
                    }

                    //  是否调用成功
                    isSuccess = buffer[++index] == 116;
                    index += (isSuccess ? 5 : 6);

                    //  计算耗时
                    elapsedTime = 0;
                    while (buffer[index] != 44) {
                        elapsedTime = elapsedTime * 10 + (buffer[index++] - 48);
                    }

                    //  计算时间
                    stamp = 0;
                    index += (11 - STAMP_LEN);
                    for (int i = 0; i < STAMP_LEN; ++i) {
                        stamp = stamp * 10 + (buffer[index++] - 48);
                    }
                    timeIdx = (stamp - STAMP_MIN) / 60;
                    index += 4;

                    //  处理
                    if (timeIdx != curTimeIdx) {
                        for (int i = 1; i < SERVICE_MAX; ++i) {
                            records = 0;
                            trueRecords = 0;
                            for (int j = 1; j < SERVICE_MAX; ++j) {
                                List<String> resList = new ArrayList<>();

                                for (int k = 1; k < IP_INDEX_MAX; ++k) {
                                    for (int l = 1; l < IP_INDEX_MAX; ++l) {
                                        if (recordData[i][j][k][l][0] == 0)
                                            continue;

                                        qps = recordData[i][j][k][l][0];
                                        trueQps = recordData[i][j][k][l][1];
                                        rateIdx = (int) ((trueQps * 10000.0) / qps);
                                        trueRecords += trueQps;
                                        records += qps;

                                        double d = qps * 0.99;
                                        p99Idx = (int) d;
                                        if (p99Idx == d)
                                            --p99Idx;
                                        for (p99 = ELAPSE_MAX - 1; p99 >= 0; --p99) {
                                            qps -= (pairData[i][j][k][l][p99]);
                                            if (p99Idx >= qps)
                                                break;
                                        }

                                        builder.setLength(0);
                                        builder.append(ip[j][k]).append(",").append(ip[i][l]).append(",").append(rate[rateIdx]).append(",").append(p99);
                                        resList.add(builder.toString());

                                        Arrays.fill(pairData[i][j][k][l], (short) 0);
                                        recordData[i][j][k][l][0] = recordData[i][j][k][l][1] = 0;
                                    }
                                }
                                checkPairRes[curTimeIdx][i][j] = resList;
                            }

                            responderData[curTimeIdx][i] = (int) ((trueRecords * 10000.0) / records);
                        }

                        curTimeIdx = timeIdx;
                    }

                    //  添加数据
                    ++pairData[responderIdx][callerIdx][callerIpIdx][responderIpIdx][elapsedTime];
                    ++recordData[responderIdx][callerIdx][callerIpIdx][responderIpIdx][0];
                    if (isSuccess)
                        ++recordData[responderIdx][callerIdx][callerIpIdx][responderIpIdx][1];
                }
            }

            //  处理最后一分钟
            for (int i = 1; i < serviceCount; ++i) {
                records = 0;
                trueRecords = 0;
                for (int j = 1; j < serviceCount; ++j) {
                    List<String> resList = new ArrayList<>();

                    for (int k = 1; k < IP_INDEX_MAX; ++k) {
                        for (int l = 1; l < IP_INDEX_MAX; ++l) {
                            if (recordData[i][j][k][l][0] == 0)
                                continue;

                            qps = recordData[i][j][k][l][0];
                            trueQps = recordData[i][j][k][l][1];
                            rateIdx = (int) ((trueQps * 10000.0) / qps);
                            trueRecords += trueQps;
                            records += qps;

                            double d = qps * 0.99;
                            p99Idx = (int) d;
                            if (p99Idx == d)
                                --p99Idx;
                            for (p99 = ELAPSE_MAX - 1; p99 >= 0; --p99) {
                                qps -= (pairData[i][j][k][l][p99]);
                                if (p99Idx >= qps)
                                    break;
                            }

                            builder.setLength(0);
                            builder.append(ip[j][k]).append(",").append(ip[i][l]).append(",").append(rate[rateIdx]).append(",").append(p99);
                            resList.add(builder.toString());
                        }
                    }
                    checkPairRes[timeIdx][i][j] = resList;
                }

                responderData[timeIdx][i] = (int) ((trueRecords * 10000.0) / records);
            }

            //  checkResponder最终结果
            for (int i = 1; i < serviceCount; ++i) {
                for (int j = 0; j < TIME_MAX; ++j) {
                    int res = 0;
                    for (int k = j; k < TIME_MAX; ++k) {
                        res += responderData[k][i];
                        checkResponderRes[i][j][k] = rate[res / (k - j + 1)];
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * @param caller    主调服务名称
     * @param responder 被调服务名称
     * @param time      需要查询的时间（分钟），格式 yyyy-MM-dd hh:mm
     * @return 返回在这一分钟内主被调按ip聚合的成功率和P99，无调用反馈空list
     */
    public List<String> checkPair(String caller, String responder, String time) {
        if (caller.charAt(caller.length() - 3) == 69 || responder.charAt(responder.length() - 3) == 69)
            return new ArrayList<>();

        int timeIdx;
        if ((timeIdx = getTimeIdx(time)) == -1)
            return new ArrayList<>();

        int callerIdx = getServiceIdx(caller), responderIdx = getServiceIdx(responder);
        return checkPairRes[timeIdx][responderIdx][callerIdx];
    }


    /**
     * @param responder 被调服务名称
     * @param start     需要查询区间的开始时间（分钟），格式 yyyy-MM-dd hh:mm
     * @param end       需要查询区间的结束时间（分钟），格式 yyyy-MM-dd hh:mm
     * @return 返回在start，end区间内responder作为被调的平均成功率
     */
    public String checkResponder(String responder, String start, String end) {
        if (responder.charAt(responder.length() - 3) == 69)
            return "-1.00%";

        int startTimeIdx = getTimeIdx(start), endTimeIdx = getTimeIdx(end);
        return checkResponderRes[getServiceIdx(responder)][startTimeIdx == -1 ? 0 : startTimeIdx][endTimeIdx == -1 ? TIME_MAX - 1 : endTimeIdx];
    }

}
