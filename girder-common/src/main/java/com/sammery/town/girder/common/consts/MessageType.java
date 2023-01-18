package com.sammery.town.girder.common.consts;

/**
 * 常量类
 * @author 沙漠渔
 */
public interface MessageType {
    /**
     * 心跳 包括传递自己有哪些连接以及服务端验证是否完整,
     * 如果完整则数据域为空
     * 如果不完整或者不正确则数据域传递需要操作类型+IP+端口 操作类型:00 删除 01 新增
     */
    byte HEART = 0x09;

    /**
     * 检测客户端id和认证码是否正确
     * 客户端ID取自mac地址
     * 认证码再讨论 可以通过客户端ID做一个加密验证  到时候在服务端验证通过即可
     */
    byte AUTH = 0x01;

    /**
     * 建立连接的请求，
     */
    byte CONNECT = 0x02;

    /** 代理后端服务器断开连接消息 */
    byte DISCONNECT = 0x03;

    /** 代理数据传输 */
    byte TRANSFER = 0x04;

}
