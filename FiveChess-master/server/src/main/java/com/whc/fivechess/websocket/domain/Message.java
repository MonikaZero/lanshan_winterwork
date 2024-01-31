package com.whc.fivechess.websocket.domain;

import cn.hutool.json.JSONUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * 封装websocket消息体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message <T>{
    private Integer code;
    private String type;
    private T content;

}
