package com.whc.fivechess.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 对战记录
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Record {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer player1;
    private String name1;
    private Integer player2;
    private String name2;
    private String start;
    private String end;
    private Integer win;  //0:平居
    private Integer steps;
    private String data;  //数据id
    private String type; // normal ：正常胜负局  peace ：平居  surrender:认输
}
