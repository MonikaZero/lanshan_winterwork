package com.whc.fivechess.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 整个对局的步数
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Steps implements Serializable {
   private List<Step> steps;
}
