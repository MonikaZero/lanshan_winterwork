package com.whc.fivechess;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.whc.fivechess.domain.*;
import com.whc.fivechess.mapper.GameMapper;
import com.whc.fivechess.mapper.RecordMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.*;

@SpringBootTest
class FiveChessApplicationTests {

    @Autowired
    private GameMapper gameMapper;
    @Autowired
    private RecordMapper recordMapper;

    @Test
    void test1(){
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Record> page = new Page<>(1,2);
        QueryWrapper<Record> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("player1",13).or().eq("player2",13);
        Page<Record> page1 = recordMapper.selectPage(page,queryWrapper);
        System.out.println(page1.getTotal());
    }

    @Test
    void contextLoads() {
        Step step1 = new Step(1,1,1,"white");
        Step step2 = new Step(2,2,2,"black");
        Steps steps = new Steps();
        steps.getSteps().add(step1);
        steps.getSteps().add(step2);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(byteArrayOutputStream);
            out.writeObject(steps);
        }catch (IOException e){
            e.printStackTrace();
        }finally {
            try{
                if (out != null) {
                    out.close();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        Game data = new Game(null,byteArrayOutputStream.toByteArray());
        gameMapper.insert(data);

    }

    @Test
    void test(){
        Game game = gameMapper.selectById(2);


        ByteArrayInputStream bais;
        ObjectInputStream in = null;
        Steps steps = null;
        try{
            bais = new ByteArrayInputStream(game.getData());
            in = new ObjectInputStream(bais);
            steps  = (Steps)in.readObject();
        }catch (Exception e){
            e.printStackTrace();
        }
        finally{
            if(in != null){
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if(steps!=null)
        for (Step step:steps.getSteps()){
            System.out.println(step.getPlayer());
        }
    }

}
