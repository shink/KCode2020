package com.kuaishou.kcode;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;

/**
 * @author kcode
 * Created on 2020-05-20
 */
public class KcodeMain {

    public static void main(String[] args) throws Exception {
        // "demo.data" 是你从网盘上下载的测试数据，这里直接填你的本地绝对路径
        InputStream fis = new FileInputStream("datasets/test.data");
        Class<?> clazz = Class.forName("com.kuaishou.kcode.KcodeQuestion");
        Object instance = clazz.newInstance();
        Method prepareMethod = clazz.getMethod("prepare", InputStream.class);
        Method getResultMethod = clazz.getMethod("getResult", Long.class, String.class);
        // 调用prepare()方法准备数据
        long startTime = System.currentTimeMillis();
        prepareMethod.invoke(instance, fis);
        long endTime = System.currentTimeMillis();
        System.out.println("prepare elapsed time：" + (endTime - startTime) / 1000 + "s");

        // 验证正确性
        // "result.data" 是你从网盘上下载的结果数据，这里直接填你的本地绝对路径
        startTime = System.currentTimeMillis();
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream("datasets/result.data")));
        String line;
        int error = 0, records = 0;

        while ((line = reader.readLine()) != null) {
            String[] split = line.split("\\|");
            String[] keys = split[0].split(",");
            // 调用getResult()方法
            Object result = getResultMethod.invoke(instance, new Long(keys[0]), keys[1]);
            if (!split[1].equals(result)) {
                System.out.println(keys[0] + ", " + keys[1]);
                System.out.println(split[1]);
                System.out.println(result);
                System.out.println("-----------------");
                ++error;
            }
            ++records;
        }
        int accuracy = (records - error) * 100 / records;

        System.out.println("records: " + records + ", error records: " + error);
        if (accuracy == 100)
            System.out.println("accuracy: 100% PASSED");
        else
            System.out.println("accuracy: " + accuracy + "% THE RESULT IS INCORRECT");

        endTime = System.currentTimeMillis();
        System.out.println("getResult elapsed time：" + (endTime - startTime) + "ms");
    }
}