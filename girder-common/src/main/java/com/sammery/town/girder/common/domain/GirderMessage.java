package com.sammery.town.girder.common.domain;

import lombok.Getter;
import lombok.Setter;

/**
 * 代理客户端与代理服务器消息交换协议
 * @author 沙漠渔
 * 2023年1月18日 10:32:55 沙漠渔 去掉不必要的字段
 */
@Getter@Setter
public class GirderMessage {

    /** 消息类型 */
    private byte type;

    /** 消息流水号 */
    private short sn;

    /** 消息传输数据 */
    private byte[] data;
}
