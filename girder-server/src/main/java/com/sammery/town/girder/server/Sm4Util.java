package com.sammery.town.girder.server;

import com.sammery.town.girder.common.utils.CommUtil;
import org.bouncycastle.crypto.digests.SM3Digest;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.pqc.math.linearalgebra.ByteUtils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.AlgorithmParameters;
import java.security.Key;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Arrays;

/**
 * @description: SM4加密
 * @author: 张璞
 * @date 10:20 2022/9/13
 */
public class Sm4Util {
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private static final String ENCODING = "UTF-8";
    public static final String ALGORITHM_NAME = "SM4";
    /**
     * 功能描述: 加密算法/分组加密模式/分组填充方式\PKCS5Padding-以8个字节为一组进行分组加密\定义分组加密模式使用：PKCS5Paddings
     */
    public static final String ALGORITHM_NAME_CBC_PADDING = "SM4/CBC/PKCS7Padding";
    /**
     * 功能描述: 128-32位16进制；256-64位16进制
     */
    public static final int DEFAULT_KEY_SIZE = 128;


    /**
     * 功能描述: 测试
     *
     * @author zhang pu
     * @date 10:31 2022/9/13
     */
    public static void main(String[] args) throws Exception {
        String s = generateKeyString();
        System.out.println("密钥：" + s);
        byte[] a = { 0x01,0x23,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
        String uuid = ByteUtils.toHexString(a);
        System.out.println("待加密数据：" + uuid);
        //sm4对称加密
        String s1 = encryptTextSm4(s, uuid);

        System.out.println("加密数据：" + s1);
        String s2 = decodeTextSm4(s, s1);
        System.out.println("解密数据：" + s2);
        //sm3加密
        String s3 = encryptTextSm3(uuid);
        System.out.println(verify(uuid, s3));
    }

    /**
     * 功能描述: sm4加密 加密模式：CBC
     * 无线局域网标准的分组数据算法。对称加密，密钥长度和分组长度均为128位
     *
     * @param hexKey   16进制密钥（忽略大小写）
     * @param paramStr 待加密字符串
     *                 返回16进制的加密字符串
     * @author zhang pu
     * @date 10:31 2022/9/13
     */
    public static String encryptTextSm4(String hexKey, String paramStr) throws Exception {
        String result = "";
        // 16进制字符串-->byte[]
        byte[] keyData = CommUtil.hex2Binary(hexKey);
        // String-->byte[]
        byte[] srcData = CommUtil.hex2Binary(paramStr);
        // 加密后的数组
        byte[] cipherArray = encrypt_Cbc_Padding(keyData, srcData);
        // byte[]-->hexString
        result = CommUtil.byteToHexString(cipherArray);
        return result;
    }

    /**
     * 功能描述: sm4解密 解密模式：采用CBC
     *
     * @param hexKey 16进制密钥
     * @param text   16进制的加密字符串（忽略大小写）
     * @author zhang pu
     * @date 10:31 2022/9/13
     */
    public static String decodeTextSm4(String hexKey, String text) throws Exception {
        byte[] keyData = CommUtil.hex2Binary(hexKey);
        byte[] resultData = CommUtil.hex2Binary(text);
        // 解密
        byte[] srcData = decrypt_Cbc_Padding(keyData, resultData);
        return CommUtil.byteToHexString(srcData);
    }

    /**
     * 功能描述: sm3加密
     * 消息摘要。可以用MD5作为对比理解。该算法已公开。校验结果为256位。
     *
     * @param text 内容
     * @author zhang pu
     * @date 10:39 2022/9/13
     */
    public static String encryptTextSm3(String text) throws UnsupportedEncodingException {
        byte[] bytes = text.getBytes(ENCODING);
        byte[] hash = hash(bytes);
        String s = ByteUtils.toHexString(hash);
        return s;
    }

    public static byte[] hash(byte[] srcData) {
        SM3Digest digest = new SM3Digest();
        digest.update(srcData, 0, srcData.length);
        byte[] bytes = new byte[digest.getDigestSize()];
        digest.doFinal(bytes, 0);
        return bytes;
    }

    /**
     * 功能描述:判断元数据与加密数据是否一致
     *
     * @author zhang pu
     * @date 10:56 2022/9/13
     * 参数 srcStr  元数据
     * 参数 sm3HexString 加密过的数据
     */
    public static boolean verify(String srcStr, String sm3HexString) throws Exception {
        boolean flag = false;
        byte[] srcStrData = srcStr.getBytes(ENCODING);
        byte[] sm3HexStringData = ByteUtils.fromHexString(sm3HexString);
        byte[] hash = hash(srcStrData);
        if (Arrays.equals(hash, sm3HexStringData)) {
            flag = true;
        }
        return flag;
    }

    /**
     * 功能描述: 生成密钥
     *
     * @author zhang pu
     * @date 10:30 2022/9/13
     */
    public static String generateKeyString() throws Exception {
        KeyGenerator kg = KeyGenerator.getInstance(ALGORITHM_NAME, BouncyCastleProvider.PROVIDER_NAME);
        kg.init(DEFAULT_KEY_SIZE, new SecureRandom());
        byte[] encoded = { (byte) 0x01,(byte) 0x23,(byte) 0x45,(byte) 0x67, (byte) 0x89, (byte) 0xab, (byte) 0xcd, (byte) 0xef, (byte) 0xfe,(byte) 0xdc,(byte) 0xba,(byte) 0x98,
                (byte) 0x76,(byte) 0x54,(byte) 0x32,(byte) 0x10 };
        return ByteUtils.toHexString(encoded);
    }

    /**
     * 加密模式之CBC
     *
     * @param key
     * @param data
     * @return
     * @throws Exception
     * @explain
     */
    public static byte[] encrypt_Cbc_Padding(byte[] key, byte[] data) throws Exception {
        Cipher cipher = generateCbcCipher(ALGORITHM_NAME_CBC_PADDING, Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(data);
    }

    private static Cipher generateCbcCipher(String algorithmName, int mode, byte[] key) throws Exception {
        Cipher cipher = Cipher.getInstance(algorithmName, BouncyCastleProvider.PROVIDER_NAME);
        Key sm4Key = new SecretKeySpec(key, ALGORITHM_NAME);
        cipher.init(mode, sm4Key, generateIV());
        return cipher;
    }

    /**
     * 功能描述: 初始化算法参数
     *
     * @author zhang pu
     * @date 10:33 2022/9/13
     */
    public static AlgorithmParameters generateIV() throws Exception {
        byte[] iv = new byte[16];
        //设置数据全为0
        Arrays.fill(iv, (byte) 0x00);
        AlgorithmParameters params = AlgorithmParameters.getInstance(ALGORITHM_NAME);
        params.init(new IvParameterSpec(iv));
        return params;
    }

    /**
     * 功能描述: cbc模式解密
     *
     * @author zhang pu
     * @date 10:33 2022/9/13
     */
    public static byte[] decrypt_Cbc_Padding(byte[] key, byte[] cipherText) throws Exception {
        Cipher cipher = generateCbcCipher(ALGORITHM_NAME_CBC_PADDING, Cipher.DECRYPT_MODE, key);
        return cipher.doFinal(cipherText);
    }
}


