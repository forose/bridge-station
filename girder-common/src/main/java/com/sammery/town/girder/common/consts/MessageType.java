package com.sammery.town.girder.common.consts;

/**
 * 常量类
 * @author 沙漠渔
 */
public interface MessageType {
    /** 心跳消息 */
    byte HEART = 0x09;

    /** 认证消息，检测clientKey是否正确 */
    byte AUTH = 0x01;

    /** 确认消息 */
    byte ACK = 0x02;

    /** 代理后端服务器建立连接消息 */
    byte CONNECT = 0x03;

    /** 代理后端服务器断开连接消息 */
    byte DISCONNECT = 0x04;

    /** 代理数据传输 */
    byte TRANSFER = 0x05;
}
