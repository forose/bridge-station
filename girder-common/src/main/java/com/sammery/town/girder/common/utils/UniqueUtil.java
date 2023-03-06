package com.sammery.town.girder.common.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;

/**
 * 获取计算机唯一识别码
 *
 * @author 沙漠渔
 * @date 2023年3月6日 08:51:41
 */
@Slf4j
public class UniqueUtil {

    /**
     * 获取计算机唯一识别码
     *
     * @author 沙漠渔
     * @date 2023年3月6日 08:51:41
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