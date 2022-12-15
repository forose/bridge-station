/*
 * Created with IntelliJ IDEA 2018.3.4.
 * Description:
 * Author: 沙漠渔
 * Date: 2020-10-28
 * Time: 8:52
 * All Rights Reserved , Copyright (C) 2018
 */
package com.sammery.town.girder.common.utils;

import org.springframework.util.StringUtils;

import java.util.Arrays;

/**
 * 说明:
 *
 * @description: 协议处理工具类
 * @program: protocol-parser
 * @author: 沙漠渔
 * @create: 2020-10-28 08:52
 **/
public class CommUtil {
    public static byte obtainCsByte(byte[] bytes, int from, int len) {
        int cs = 0;
        for (int i = from; i < from + len; i++) {
            cs += bytes[i];
        }
        return (byte) (cs % 256);
    }

    /**
     * 将字节数组转为十六进制字符串
     *
     * @param bytes 字节数组
     * @return string
     */
    public static String byteToHexString(byte[] bytes) {
        String result = "";
        if (bytes != null) {
            result = byteToHexString(bytes, 0, bytes.length);
        }
        return result;
    }

    public static String byteToHexStringWithBlank(byte[] bytes) {
        return addBlankEveryTwo(byteToHexString(bytes));
    }

    public static String byteToHexStringWithBlank(byte[] bytes, int from, int len) {
        return addBlankEveryTwo(byteToHexString(bytes, from, len));
    }

    /**
     * 指定字节数组中的位置转为十六进制字符串
     *
     * @param bytes 报文
     * @param from  起始位
     * @param len   长度
     * @return 16进制字符串
     */
    public static String byteToHexString(byte[] bytes, int from, int len) {
        StringBuilder sb = new StringBuilder();
        if (bytes != null) {
            for (int i = from; i < from + len; i++) {
                sb.append(byteToHexString(bytes[i]));
            }
        }
        return sb.toString();
    }

    public static String byteToAsciiString(byte[] bytes, int from, int len) {
        return new String(Arrays.copyOfRange(bytes, from, from + len));
    }

    public static String byteToHexString(int number) {
        String str = Integer.toHexString(number & 0xFF);
        StringBuilder bytes = new StringBuilder();
        if (str.length() == 1) {
            bytes.append("0");
        }
        return bytes.append(str).toString().toUpperCase();
    }

    /**
     * 指定字节数组中的位置转为十六进制字符串
     *
     * @param bytes 字节数组
     * @param from  起始位
     * @param len   长度
     * @return 高低位倒置的16进制字符串
     */
    public static String byteToHexStringConverse(byte[] bytes, int from, int len) {
        StringBuilder sb = new StringBuilder();
        if (bytes != null) {
            String hexString;
            for (int i = from + len - 1; i >= from; i--) {
                hexString = Integer.toHexString(bytes[i] & 0xFF);
                if (hexString.length() == 1) {
                    sb.append("0");
                }
                sb.append(hexString);
            }
        }
        return sb.toString();
    }

    public static String hexConverse(String input) {
        if (StringUtils.isEmpty(input)) {
            return input;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = (input.length() / 2 - 1); i >= 0; i--) {
            builder.append(input.charAt(2 * i)).append(input.charAt(2 * i + 1));
        }
        return builder.toString();
    }

    /**
     * 六进制形式字符串转化为Byte数组，正向
     *
     * @param hexString 字符串
     * @return 返回数组
     */
    public static byte[] hex2Binary(String hexString) {
        int len = hexString.length() / 2;
        byte[] bytes = new byte[len];
        for (int i = 0; i < len; i++) {
            bytes[len - 1 - i] = (byte) Integer.parseInt(hexString.substring((len - i - 1) * 2, (len - i) * 2), 16);
        }
        return bytes;
    }

    /**
     * 每隔两个字母 增加一个空格
     *
     * @param replace 需要处理的字符串
     * @return 增加空格的字符串
     */
    private static String addBlankEveryTwo(String replace) {
        return replace.replaceAll("(.{2})", "$1 ");
    }
}
