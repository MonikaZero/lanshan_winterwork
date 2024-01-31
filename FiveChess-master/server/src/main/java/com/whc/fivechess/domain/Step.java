package com.whc.fivechess.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 一步
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Step implements Serializable {
    private Integer player; //棋手id
    private Integer x; //横坐标
    private Integer y; //纵坐标
    private String color; //棋子颜色
}
