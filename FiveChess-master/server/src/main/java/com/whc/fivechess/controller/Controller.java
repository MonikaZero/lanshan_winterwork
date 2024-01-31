package com.whc.fivechess.controller;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.whc.fivechess.domain.*;
import com.whc.fivechess.mapper.GameMapper;
import com.whc.fivechess.mapper.RecordMapper;
import com.whc.fivechess.mapper.UserMapper;
import com.whc.fivechess.util.MD5;
import com.whc.fivechess.util.MyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Date;
import java.util.List;

@RestController
public class Controller {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private RecordMapper recordMapper;
    @Autowired
    private GameMapper gameMapper;

    @PostMapping("/loginPost")
    public String login(@RequestBody String request){
        JSONObject data = JSONUtil.parseObj(request);
        String login = data.getStr("login");
        String password = data.getStr("password");
        if(login==null||password==null){
            return JSONUtil.toJsonStr(new ResponseDto<>(false,400,"账户或密码为空！"));
        }

        QueryWrapper<User> query = new QueryWrapper<>();
        query.eq("login",login);
        User user = userMapper.selectOne(query);
        if(user==null){
            return JSONUtil.toJsonStr(new ResponseDto<>(false,401,"账户未注册！"));
        }
        if(!user.getPassword().equals(MD5.toMD5(password))){
            return JSONUtil.toJsonStr(new ResponseDto<>(false,402,"密码错误！"));
        }
        return JSONUtil.toJsonStr(new ResponseDto<>(user));
    }

    @PostMapping("/registerPost")
    public String register(@RequestBody String request){
        JSONObject data = JSONUtil.parseObj(request);
        String id = data.getStr("id");
        String name = data.getStr("name");
        String password = data.getStr("password");

        if(id==null||password==null||name==null){
            return JSONUtil.toJsonStr(new ResponseDto<>(false,400,"账户或密码或昵称为空！"));
        }
        QueryWrapper<User> query = new QueryWrapper<>();
        query.eq("login",id);
        User user = userMapper.selectOne(query);
        if(user!=null){
            return JSONUtil.toJsonStr(new ResponseDto<>(false,401,"账号已被注册！"));
        }
        User user1 = new User(null,id,name,MD5.toMD5(password), MyUtil.simpleDateFormat.format(new Date()));

        userMapper.insert(user1);
        return JSONUtil.toJsonStr(new ResponseDto<>("注册成功！"));
    }

    //以分页的方式获取对局列表
    @PostMapping("/getPage")
    public String getPage(@RequestBody String request){
        JSONObject data = JSONUtil.parseObj(request);
        String id = data.getStr("id");
        String pageNum = data.getStr("pageNum"); //页码

        Page<Record> page = new Page<>(Integer.parseInt(pageNum),8);
        QueryWrapper<Record> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("player1",id).or().eq("player2",id).orderByDesc("start");
        return JSONUtil.toJsonStr(new ResponseDto<>(recordMapper.selectPage(page,queryWrapper)));
    }
    //获取对局列表
    @PostMapping("/getRecords")
    public String getRecords(@RequestBody String request){
        JSONObject data = JSONUtil.parseObj(request);
        String id = data.getStr("id");
        //查找该用户的全部对局
        QueryWrapper<Record> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("player1",id).or().eq("player2",id).orderByDesc("start");
        List<Record> records = recordMapper.selectList(queryWrapper);
        return JSONUtil.toJsonStr(new ResponseDto<>(records));
    }

    //获取某一对局详情
    @PostMapping("/getGameData")
    public String getGameData(@RequestBody String request){
        JSONObject data = JSONUtil.parseObj(request);
        String id = data.getStr("id");

        Game game = gameMapper.selectById(id);
        if(game==null){
            return JSONUtil.toJsonStr(new ResponseDto<>(false,400,"未找到对局！"));
        }

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
        if(steps==null){
            return JSONUtil.toJsonStr(new ResponseDto<>(false,400,"对局查找出错！"));
        }
        return JSONUtil.toJsonStr(new ResponseDto<>(steps));
    }
}
