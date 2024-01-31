package com.whc.fivechess.websocket.domain;

import com.whc.fivechess.domain.Step;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * 对战房间
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Room {
    private String id = UUID.randomUUID().toString();//初始化房间号
    private String playerId1; //棋手1id  房主
    private String playerId2 = "-1"; //棋手2id   创建时没人
    private String playerName1; //棋手1 昵称
    private String playerName2 = ""; //棋手2昵称
    private String state = "waiting"; //默认状态名字
    ///开局之后才会有的数据
    private String start = null; //开局时间
    private List<Step> steps = null;    //记录
    private Integer winCheck = 0;    //初始为0 一方确认赢了之后+1  到2的时候保存对局记录
}
