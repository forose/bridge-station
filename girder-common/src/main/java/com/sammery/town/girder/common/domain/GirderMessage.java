package com.sammery.town.girder.common.domain;

import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;

/**
 * 代理客户端与代理服务器消息交换协议
 * @author forose
 */
@Getter@Setter
public class GirderMessage {

    /** 68 */
    private byte head;

    /** 报文长度 包含去掉68和16之后的所有 */
    private int length;

    /** 消息类型 */
    private byte type;

    /** 消息流水号 */
    private int sn;

    /** 消息传输数据 */
    private byte[] data;

    /**
     * 消息的crc校验 去掉68之后到校验位前的所有部分 crc16/x25校验
     */
    private byte[] crc;

    /**
     * 消息16结尾
     */
    private byte tail;
}
