package com.whc.fivechess.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.whc.fivechess.domain.Game;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface GameMapper extends BaseMapper<Game> {
}
