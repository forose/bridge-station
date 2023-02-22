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
     * Windows
     */
    public static final String WINDOWS = "windows";
    /**
     * Linux
     */
    public static final String LINUX = "linux";
    /**
     * Unix
     */
    public static final String UNIX = "unix";
    /**
     * 正则表达式
     */
    public static final String REGEX = "\\b\\w+:\\w+:\\w+:\\w+:\\w+:\\w+\\b";
 
    /**
     * 获取 Windows 主板序列号
     *
     * @return String - 计算机主板序列号
     * @author XinLau
     * @creed The only constant is change ! ! !
     * @since 2020/10/10 17:15
     */
    private static String getWindowsMainBoardSerialNumber() {
        String result = "";
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
                result += line;
            }
            input.close();
        } catch (Exception e) {
            log.error("获取 Windows 主板信息错误", e);
        }
        return result.trim();
    }
 
    /**
     * 获取 Linux 主板序列号
     *
     * @return String - 计算机主板序列号
     * @author XinLau
     * @creed The only constant is change ! ! !
     * @since 2020/10/10 17:15
     */
    private static String getLinuxMainBoardSerialNumber() {
        String result = "";
        String maniBord_cmd = "dmidecode | grep 'Serial Number' | awk '{print $3}' | tail -1";
        Process p;
        try {
            // 管道
            p = Runtime.getRuntime().exec(new String[]{"sh", "-c", maniBord_cmd});
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                result += line;
                break;
            }
            br.close();
        } catch (IOException e) {
            log.error("获取 Linux 主板信息错误", e);
        }
        return result;
    }
 
    /**
     * 从字节获取 MAC
     *
     * @param bytes - 字节
     * @return String - MAC
     * @author XinLau
     * @creed The only constant is change ! ! !
     * @since 2020/10/12 8:55
     */
    private static String getMacFromBytes(byte[] bytes) {
        StringBuffer mac = new StringBuffer();
        byte currentByte;
        boolean first = false;
        for (byte b : bytes) {
            if (first) {
                mac.append("-");
            }
            currentByte = (byte) ((b & 240) >> 4);
            mac.append(Integer.toHexString(currentByte));
            currentByte = (byte) (b & 15);
            mac.append(Integer.toHexString(currentByte));
            first = true;
        }
        return mac.toString().toUpperCase();
    }
 
    /**
     * 获取 Windows 网卡的 MAC 地址
     *
     * @return String - MAC 地址
     * @author XinLau
     * @creed The only constant is change ! ! !
     * @since 2020/10/10 17:15
     */
    private static String getWindowsMACAddress() {
        InetAddress ip = null;
        NetworkInterface ni = null;
        List<String> macList = new ArrayList<String>();
        try {
            Enumeration<NetworkInterface> netInterfaces = (Enumeration<NetworkInterface>) NetworkInterface
                    .getNetworkInterfaces();
            while (netInterfaces.hasMoreElements()) {
                ni = (NetworkInterface) netInterfaces.nextElement();
                //  遍历所有 IP 特定情况，可以考虑用 ni.getName() 判断
                Enumeration<InetAddress> ips = ni.getInetAddresses();
                while (ips.hasMoreElements()) {
                    ip = (InetAddress) ips.nextElement();
                    // 非127.0.0.1
                    if (!ip.isLoopbackAddress() && ip.getHostAddress().matches("(\\d{1,3}\\.){3}\\d{1,3}")) {
                        macList.add(getMacFromBytes(ni.getHardwareAddress()));
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取 Windows MAC 错误", e);
        }
        if (macList.size() > 0) {
            return macList.get(0);
        } else {
            return "";
        }
    }
 
    /**
     * 获取 Linux 网卡的 MAC 地址 （如果 Linux 下有 eth0 这个网卡）
     *
     * @return String - MAC 地址
     * @author XinLau
     * @creed The only constant is change ! ! !
     * @since 2020/10/10 17:15
     */
    private static String getLinuxMACAddressForEth0() {
        String mac = null;
        BufferedReader bufferedReader = null;
        Process process = null;
        try {
            // Linux下的命令，一般取eth0作为本地主网卡
            process = Runtime.getRuntime().exec("ifconfig eth0");
            // 显示信息中包含有 MAC 地址信息
            bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = null;
            int index = -1;
            while ((line = bufferedReader.readLine()) != null) {
                // 寻找标示字符串[hwaddr]
                index = line.toLowerCase().indexOf("hwaddr");
                if (index >= 0) {
                    // // 找到并取出 MAC 地址并去除2边空格
                    mac = line.substring(index + "hwaddr".length() + 1).trim();
                    break;
                }
            }
        } catch (IOException e) {
            log.error("获取 Linux MAC 信息错误", e);
        } finally {
            try {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
            } catch (IOException e1) {
                log.error("获取 Linux MAC 信息错误", e1);
            }
            bufferedReader = null;
            process = null;
        }
        return mac;
    }
 
    /**
     * 获取 Linux 网卡的 MAC 地址
     *
     * @return String - MAC 地址
     * @author XinLau
     * @creed The only constant is change ! ! !
     * @since 2020/10/10 17:15
     */
    private static String getLinuxMACAddress() {
        String mac = null;
        BufferedReader bufferedReader = null;
        Process process = null;
        try {
            // Linux下的命令 显示或设置网络设备
            process = Runtime.getRuntime().exec("ifconfig");
            // 显示信息中包含有 MAC 地址信息
            bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = null;
            int index = -1;
            while ((line = bufferedReader.readLine()) != null) {
                Pattern pat = Pattern.compile(REGEX);
                Matcher mat = pat.matcher(line);
                if (mat.find()) {
                    mac = mat.group(0);
                }
            }
        } catch (IOException e) {
            log.error("获取 Linux MAC 信息错误", e);
        } finally {
            try {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
            } catch (IOException e1) {
                log.error("获取 Linux MAC 信息错误", e1);
            }
            bufferedReader = null;
            process = null;
        }
        return mac;
    }
 
    /**
     * 获取 Windows 的 CPU 序列号
     *
     * @return String - CPU 序列号
     * @author XinLau
     * @creed The only constant is change ! ! !
     * @since 2020/10/10 17:15
     */
    private static String getWindowsProcessorIdentification() {
        StringBuilder result = new StringBuilder();
        try {
            File file = File.createTempFile("tmp", ".vbs");
            file.deleteOnExit();
            FileWriter fw = new java.io.FileWriter(file);
            String vbs = "Set objWMIService = GetObject(\"winmgmts:\\\\.\\root\\cimv2\")\n"
                    + "Set colItems = objWMIService.ExecQuery _ \n" + "   (\"Select * from Win32_Processor\") \n"
                    + "For Each objItem in colItems \n" + "    Wscript.Echo objItem.ProcessorId \n"
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
            log.error("获取 Windows CPU 信息错误", e);
        }
        return result.toString().trim();
    }
 
    /**
     * 获取 Linux 的 CPU 序列号
     *
     * @return String - CPU 序列号
     * @author XinLau
     * @creed The only constant is change ! ! !
     * @since 2020/10/10 17:15
     */
    private static String getLinuxProcessorIdentification() {
        String result = "";
        String CPU_ID_CMD = "dmidecode";
        BufferedReader bufferedReader = null;
        Process p = null;
        try {
            // 管道
            p = Runtime.getRuntime().exec(new String[]{"sh", "-c", CPU_ID_CMD});
            bufferedReader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = null;
            int index = -1;
            while ((line = bufferedReader.readLine()) != null) {
                index = line.toLowerCase().indexOf("uuid");
                if (index >= 0) {
                    result = line.substring(index + "uuid".length() + 1).trim();
                    break;
                }
            }
        } catch (IOException e) {
            log.error("获取 Linux CPU 信息错误", e);
        }
        return result.trim();
    }
 
    /**
     * 获取当前计算机操作系统名称 例如:windows,Linux,Unix等.
     *
     * @return String - 计算机操作系统名称
     * @author XinLau
     * @creed The only constant is change ! ! !
     * @since 2020/10/10 17:15
     */
    public static String getOSName() {
        return System.getProperty("os.name").toLowerCase();
    }
 
    /**
     * 获取当前计算机操作系统名称前缀 例如:windows,Linux,Unix等.
     *
     * @return String - 计算机操作系统名称
     * @author XinLau
     * @creed The only constant is change ! ! !
     * @since 2020/10/10 17:15
     */
    public static String getOSNamePrefix() {
        String name = getOSName();
        if (name.startsWith(WINDOWS)) {
            return WINDOWS;
        } else if (name.startsWith(LINUX)) {
            return LINUX;
        } else if (name.startsWith(UNIX)) {
            return UNIX;
        } else {
            return null;
        }
    }
 
    /**
     * 获取当前计算机主板序列号
     *
     * @return String - 计算机主板序列号
     * @author XinLau
     * @creed The only constant is change ! ! !
     * @since 2020/10/10 17:15
     */
    public static String getMainBoardSerialNumber() {
        switch (getOSNamePrefix()) {
            case WINDOWS:
                return getWindowsMainBoardSerialNumber();
            case LINUX:
                return getLinuxMainBoardSerialNumber();
            default:
                return null;
        }
    }
 
    /**
     * 获取当前计算机网卡的 MAC 地址
     *
     * @return String - 网卡的 MAC 地址
     * @author XinLau
     * @creed The only constant is change ! ! !
     * @since 2020/10/10 17:15
     */
    public static String getMACAddress() {
        switch (getOSNamePrefix()) {
            case WINDOWS:
                return getWindowsMACAddress();
            case LINUX:
                String macAddressForEth0 = getLinuxMACAddressForEth0();
                if (StringUtils.isEmpty(macAddressForEth0)) {
                    macAddressForEth0 = getLinuxMACAddress();
                }
                return macAddressForEth0;
            default:
                return null;
        }
    }
 
    /**
     * 获取当前计算机的 CPU 序列号
     *
     * @return String - CPU 序列号
     * @author XinLau
     * @creed The only constant is change ! ! !
     * @since 2020/10/10 17:15
     */
    public static String getCPUIdentification() {
        switch (Objects.requireNonNull(getOSNamePrefix())) {
            case WINDOWS:
                return getWindowsProcessorIdentification();
            case LINUX:
                return getLinuxProcessorIdentification();
            default:
                return null;
        }
    }
 
    /**
     * 获取计算机唯一标识
     *
     * @return ComputerUniqueIdentification - 计算机唯一标识
     * @author XinLau
     * @creed The only constant is change ! ! !
     * @since 2020/10/14 8:50
     */
    public static ComputerUniqueIdentification getComputerUniqueIdentification() {
        return new ComputerUniqueIdentification(getOSNamePrefix(), getMainBoardSerialNumber(), getMACAddress(), getCPUIdentification());
    }
 
    /**
     * 获取计算机唯一标识
     *
     * @return String - 计算机唯一标识
     * @author XinLau
     * @creed The only constant is change ! ! !
     * @since 2020/10/14 8:50
     */
    public static String getComputerUniqueIdentificationString() {
        return getComputerUniqueIdentification().toString();
    }
 
    /**
     * 计算机唯一标识
     */
    @Data
    private static class ComputerUniqueIdentification {
        private String namePrefix;
        private String mainBoardSerialNumber;
        private String MACAddress;
        private String CPUIdentification;
 
        public ComputerUniqueIdentification(String namePrefix, String mainBoardSerialNumber, String MACAddress, String CPUIdentification) {
            this.namePrefix = namePrefix;
            this.mainBoardSerialNumber = mainBoardSerialNumber;
            this.MACAddress = MACAddress;
            this.CPUIdentification = CPUIdentification;
        }
 
        @Override
        public String toString() {
            return new StringBuilder().append('{')
                    .append("\"namePrefix=\":\"").append(namePrefix).append("\",")
                    .append("\"mainBoardSerialNumber=\":\"").append(mainBoardSerialNumber).append("\",")
                    .append("\"MACAddress=\":\"").append(MACAddress).append("\",")
                    .append("\"CPUIdentification=\":\"").append(CPUIdentification)
                    .append("\"}").toString();
        }
    }

    public static void main(String[] args) {
        System.out.println(getComputerUniqueIdentificationString());
    }
}