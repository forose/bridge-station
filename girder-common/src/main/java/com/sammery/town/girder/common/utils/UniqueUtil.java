package com.sammery.town.girder.common.utils;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.io.*;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * UniqueUtil<br>
 * 计算机唯一识别实用程序
 * </p>
 *
 * @author XinLau
 * @version 1.0
 * @since 2020年10月10日 17:14
 */
@Slf4j
public class UniqueUtil {


    /**
     * 获取 Windows 主板序列号
     *
     * @return String - 计算机主板序列号
     * @author XinLau
     * @creed The only constant is change ! ! !
     * @since 2020/10/10 17:15
     */
    public static String getBoardSerialNumber() {
        StringBuilder result = new StringBuilder();
        try {
            File file = File.createTempFile("realhowto", ".vbs");
            file.deleteOnExit();
            FileWriter fw = new java.io.FileWriter(file);
            String vbs = "Set objWMIService = GetObject(\"winmgmts:\\\\.\\root\\cimv2\")\n"
                    + "Set colItems = objWMIService.ExecQuery _ \n" + "   (\"Select * from Win32_BaseBoard\") \n"
                    + "For Each objItem in colItems \n" + "    Wscript.Echo objItem.SerialNumber \n"
                    + "    exit for  ' do the first cpu only! \n" + "Next \n";

            fw.write(vbs);
            fw.close();
            Process p = Runtime.getRuntime().exec("cscript //NoLogo " + file.getPath());
            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = input.readLine()) != null) {
                result.append(line);
            }
            input.close();
            file.delete();
        } catch (Exception e) {
            log.error("获取 Windows 主板信息错误", e);
        }
        return result.toString().trim();
    }
}