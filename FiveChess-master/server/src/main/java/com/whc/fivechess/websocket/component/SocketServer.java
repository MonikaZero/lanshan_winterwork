package com.whc.fivechess.websocket.component;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;


import com.whc.fivechess.domain.Game;
import com.whc.fivechess.domain.Record;
import com.whc.fivechess.domain.Step;
import com.whc.fivechess.domain.Steps;
import com.whc.fivechess.mapper.GameMapper;
import com.whc.fivechess.mapper.RecordMapper;
import com.whc.fivechess.mapper.UserMapper;
import com.whc.fivechess.util.MyUtil;
import com.whc.fivechess.websocket.domain.Message;
import com.whc.fivechess.websocket.domain.Room;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.*;

import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/socket/{id}")
@Component
public class SocketServer {

    ///用来保存,用户的session
    public static Map<String, Session> sessionMap = new ConcurrentHashMap<>();
    //用来保存对战房间
    public static Map<String, Room> roomMap = new ConcurrentHashMap<>();
    //用来保存用户id对应的房间号
    public static Map<String, String> playerRoomMap = new ConcurrentHashMap<>();

    private static GameMapper gameMapper;
    private static RecordMapper recordMapper;

    @Autowired
    public void setGameMapper(GameMapper gameMapper){
        SocketServer.gameMapper = gameMapper;
    }
    @Autowired
    public void setRecordMapper(RecordMapper recordMapper){
        SocketServer.recordMapper = recordMapper;
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("id")String id){
        //检查用户是否已经登录
        if(sessionMap.containsKey(id)){
            this.sendMessage(JSONUtil.toJsonStr(new Message<>(400,"kicked","你账号已经在别处登录")),sessionMap.get(id));
        }

        sessionMap.put(id,session);
        System.out.println("用户："+id+"已连接服务器\t"+ MyUtil.simpleDateFormat.format(new Date()));

        //将在线的全部房间发给用户
        Message<Collection<Room>> message = new Message<>(200,"online-rooms",roomMap.values());
        this.sendMessage(JSONUtil.toJsonStr(message),session);


    }
    @OnError
    public void onError(Session session,Throwable error,@PathParam("id") String id){
        System.out.println("用户："+id+"已异常下线！\t"+ MyUtil.simpleDateFormat.format(new Date()));
        System.out.println("异常原因：");
        error.printStackTrace();
        Session cacheSession = sessionMap.get(id);
        if(cacheSession!=null){
            if(cacheSession.isOpen()){
                try {
                    cacheSession.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            sessionMap.remove(id);
        }
        this.quitRoom(id);
    }
    @OnClose
    public void onClose(Session session,@PathParam("id") String id){
        System.out.println("用户："+id+"已正常下线\t"+ MyUtil.simpleDateFormat.format(new Date()));
        Session cacheSession = sessionMap.get(id);
        if(cacheSession!=null){
            if(cacheSession.isOpen()){
                try {
                    cacheSession.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            sessionMap.remove(id);
        }
        //需要退出房间了
        this.quitRoom(id);
    }
    @OnMessage
    public void onMessage(String message,Session session,@PathParam("id") String id){
        JSONObject data = JSONUtil.parseObj(message);
        ///主要逻辑
        switch (data.getStr("type")){
            case "create-room":
                this.createRoom(data,session);
                break;
            case "quit-room":
                this.quitRoom(data.getStr("userId"));
                break;
            case "join-room":
                this.joinRoom(data,session);
                break;
            case "chat-message":
                this.sendRoomMessage(data);
                break;
            case "info-ready":
            case "regret-reject":
            case "regret":
            case "peace":
            case "peace-reject":
            case "restart":
            case "restart-reject":
                this.sendMessageById(data.getStr("to"),message);
                break;
            case "start-game":
                this.startGame(data.getStr("roomId"));
                break;
            case "play":
                this.play(data);
                break;
            case "regret-agree":
                this.regret(data);
                break;
            case "winCheck":
                this.winCheck(data);
                break;
            case "surrender":
                this.surrender(data);
                break;
            case "peace-agree":
                this.peaceAgree(data);
                break;
            case "restart-agree":
                this.restart(data);
                break;
            default:
                System.out.println(message);
        }
    }
    //处理重新开局
    private void restart(JSONObject data){
        String roomId = data.getStr("roomId");
        String from = data.getStr("from");
        String to = data.getStr("to");
        Room room = roomMap.get(roomId);
        //丢弃数据
        room.setSteps(new ArrayList<>());
        //新开局时间
        room.setStart(MyUtil.simpleDateFormat.format(new Date()));
        room.setWinCheck(0);
        System.out.println("房间："+roomId+"\t重新开局");
        //随机分配红白棋子
        String color1,color2;
        int random = new Random().nextInt(2);
        color1=random==0?"white":"black";
        color2=random==0?"black":"white";

        //通知玩家可以开始游戏了
        this.sendMessageById(from,
                JSONUtil.toJsonStr(new Message<>(200,"start-game",color1)));
        this.sendMessageById(to,
                JSONUtil.toJsonStr(new Message<>(200,"restart-agree",color2)));
    }
    //处理和棋
    private void peaceAgree(JSONObject data){
        String roomId = data.getStr("roomId");
        //String from = data.getStr("from");
        String to = data.getStr("to");
        Room room = roomMap.get(roomId);

        //如果小于3回合 丢弃数据
        if(room.getSteps().size()<6){
            room.setSteps(null);
        }else{
            //保存数据
            System.out.println("开始保存对局记录···");
            Steps steps = new Steps(room.getSteps());
            ObjectOutputStream out = null;
            ByteArrayOutputStream bas = MyUtil.getByteArrayOutputStream();
            try {
                out = new ObjectOutputStream(bas);
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
            String gameId = UUID.randomUUID().toString();
            Game game = new Game(gameId,bas.toByteArray());
            //数据保存到数据库
            gameMapper.insert(game);
            //记录也保存到数据
            Record record = new Record(null,Integer.valueOf(room.getPlayerId1()),room.getPlayerName1(),
                    Integer.valueOf(room.getPlayerId2()),room.getPlayerName2(),
                    room.getStart(),MyUtil.simpleDateFormat.format(new Date()),0,
                    steps.getSteps().size(),gameId,"peace");
            recordMapper.insert(record);
            System.out.println("对局记录已保存。");
            //清空房间对局记录
            room.setSteps(null);
            room.setWinCheck(0);
        }
        this.sendMessageById(to,JSONUtil.toJsonStr(data));
    }
    //认输
    private void surrender(JSONObject data){
        String roomId = data.getStr("roomId");
        //String from = data.getStr("from");
        String to = data.getStr("to");
        Room room = roomMap.get(roomId);

        //如果小于3回合 丢弃数据
        if(room.getSteps().size()<6){
            room.setSteps(null);
        }else{
            //保存数据
            System.out.println("开始保存对局记录···");
            Steps steps = new Steps(room.getSteps());
            ByteArrayOutputStream bos = MyUtil.getByteArrayOutputStream();
            ObjectOutputStream out = null;
            try {
                out = new ObjectOutputStream(bos);
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
            String gameId = UUID.randomUUID().toString();
            Game game = new Game(gameId,bos.toByteArray());
            //数据保存到数据库
            gameMapper.insert(game);
            //记录也保存到数据
            Record record = new Record(null,Integer.valueOf(room.getPlayerId1()),room.getPlayerName1(),
                    Integer.valueOf(room.getPlayerId2()),room.getPlayerName2(),
                    room.getStart(),MyUtil.simpleDateFormat.format(new Date()),Integer.valueOf(to),
                    steps.getSteps().size(),gameId,"surrender");
            recordMapper.insert(record);
            System.out.println("对局记录已保存。");
            //清空房间对局记录
            room.setSteps(null);
            room.setWinCheck(0);
        }
        this.sendMessageById(to,JSONUtil.toJsonStr(data));
    }


    private void winCheck(JSONObject data){
        String roomId = data.getStr("roomId");
        Room room = roomMap.get(roomId);
        room.setWinCheck(room.getWinCheck()+1);
        //如果双方都确认的话  则保存记录
        if(room.getWinCheck()==2){
            System.out.println("开始保存对局记录···");
            Steps steps = new Steps(room.getSteps());
            ObjectOutputStream out = null;
            ByteArrayOutputStream bas = MyUtil.getByteArrayOutputStream();
            try {
                out = new ObjectOutputStream(bas);
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
            String gameId = UUID.randomUUID().toString();
            Game game = new Game(gameId,bas.toByteArray());
            //数据保存到数据库
            gameMapper.insert(game);
            //记录也保存到数据
            Record record = new Record(null,Integer.valueOf(room.getPlayerId1()),room.getPlayerName1(),
                                        Integer.valueOf(room.getPlayerId2()),room.getPlayerName2(),
                    room.getStart(),MyUtil.simpleDateFormat.format(new Date()),Integer.valueOf(data.getStr("winId")),
                    steps.getSteps().size(),gameId,"normal");
            recordMapper.insert(record);
            System.out.println("对局记录已保存。");
            //清空房间对局记录
            room.setSteps(null);
            room.setWinCheck(0);
        }
    }
    //悔棋
    private void regret(JSONObject data){
        String roomId = data.getStr("roomId");
        int backName = Integer.parseInt(data.getStr("backNum"));
        Room room = roomMap.get(roomId);
        room.getSteps().remove(room.getSteps().size()-1);
        if(backName==2){
            room.getSteps().remove(room.getSteps().size()-1);
        }
        this.sendMessageById(data.getStr("to"),JSONUtil.toJsonStr(data));
    }
    //下棋指令
    private void play(JSONObject data){
        String from  = data.getStr("from");
        String to = data.getStr("to");
        //房间的步数++
        Room room = roomMap.get(data.getStr("roomId"));
        Integer x = Integer.valueOf(data.getStr("x"));
        Integer y = Integer.valueOf(data.getStr("y"));
        Step step = new Step(Integer.valueOf(from),x,y,data.getStr("color"));
        room.getSteps().add(step);
        //转发给对手
        this.sendMessageById(to,JSONUtil.toJsonStr(data));
    }
    //开始游戏
    private void startGame(String roomId){
        Room room = roomMap.get(roomId);
        String id1 = room.getPlayerId1();
        String id2 = room.getPlayerId2();
        //随机分配红白棋子
        String color1,color2;
        Integer random = new Random().nextInt(2);
        color1=random==0?"white":"black";
        color2=random==0?"black":"white";
        room.setSteps(new ArrayList<>()); //初始化
        room.setStart(MyUtil.simpleDateFormat.format(new Date()));

        //通知玩家可以开始游戏了
        this.sendMessageById(id1,
                JSONUtil.toJsonStr(new Message<>(200,"start-game",color1)));
        this.sendMessageById(id2,
                JSONUtil.toJsonStr(new Message<>(200,"start-game",color2)));
    }
    //房间消息
    private void sendRoomMessage(JSONObject data){
        String roomId = data.getStr("roomId");
        if(!roomMap.containsKey(roomId)) return;
        Room room = roomMap.get(roomId);
        if(room==null) return;
        String id1 = room.getPlayerId1();
        String id2 = room.getPlayerId2();
        if(id1!=null)   this.sendMessageById(id1,JSONUtil.toJsonStr(data));
        if(id2!=null)   this.sendMessageById(id2,JSONUtil.toJsonStr(data));
    }
    //加入房间
    private void joinRoom(JSONObject data,Session session){
        JSONObject user = JSONUtil.parseObj(data.getStr("user"));
        String roomId = data.getStr("roomId");
        //查找有没有这个房间的信息
        Room room = roomMap.get(roomId);
        if(room==null) {
            this.sendMessage(
                    JSONUtil.toJsonStr(
                            new Message<>(400,"join-room-fail","房间已不存在！")),
                    session);
            return;
        }
        //检查房间是否处于waiting状态
        if(room.getState().equals("playing")){
            this.sendMessage(
                    JSONUtil.toJsonStr(
                            new Message<>(400,"join-room-fail","该房间已经满了！")),
                    session);
            return;
        }
        //更新房间的信息
        room.setPlayerId2(user.getStr("id"));
        room.setPlayerName2(user.getStr("name"));
        room.setState("playing");
        playerRoomMap.put(user.getStr("id"),roomId);
        //通知房主有人加入房间了
        Message<Room> message = new Message<>(200,"someone-join-room",room);
        this.sendMessageById(room.getPlayerId1(),JSONUtil.toJsonStr(message));
        //通知申请者加入房间成功了
        message.setType("join-room-success");
        this.sendMessage(JSONUtil.toJsonStr(message),session);
        //将在线的全部房间发给全部在线用户
        Message<Collection<Room>> message1 = new Message<>(200,"online-rooms",roomMap.values());
        this.sendAllMessage(JSONUtil.toJsonStr(message1));
    }

    //退出房间
    private void quitRoom(String id){
        //先检查
        String roomId = playerRoomMap.get(id);
        if(roomId==null) return;
        Room room = roomMap.get(roomId);
        if(room==null){
            playerRoomMap.remove(id);
            return;
        }
        System.out.println("用户:"+id+"退出了房间:"+roomId);
        if(room.getPlayerId1().equals(id)){
            //房主退了  如果有房客则 房客成为房主
            playerRoomMap.remove(id);

            String player2 = room.getPlayerId2();
            if(!player2.equals("-1")){
                room.setPlayerId1(player2);
                room.setPlayerName1(room.getPlayerName2());
                room.setPlayerId2("-1");
                room.setPlayerName2("");
                room.setState("waiting");
                room.setWinCheck(0);
                room.setSteps(null);
                room.setStart(null);
                //告诉房客
                this.sendMessageById(player2,JSONUtil.toJsonStr(new Message<>(200,"become-owner",room)));
                //房主
                System.out.println("房客成为了房间:"+roomId+"的房主");
            }else{
                roomMap.remove(roomId);
            }
        }else{
            playerRoomMap.remove(id);
            //房客退了
            room.setPlayerId2("-1");
            room.setPlayerName2("");
            room.setStart(null);
            room.setState("waiting");
            room.setSteps(null);
            room.setWinCheck(0);
            //告诉房主房客退了

            this.sendMessageById(room.getPlayerId1(),JSONUtil.toJsonStr(new Message<>(200,"client-leave",room)));

        }

        //将在线的全部房间发给全部在线用户
        Message<Collection<Room>> message = new Message<>(200,"online-rooms",roomMap.values());
        this.sendAllMessage(JSONUtil.toJsonStr(message));
    }
    //创建房间
    private void createRoom(JSONObject data,Session session){
        JSONObject user = JSONUtil.parseObj(data.getStr("user"));
        //检查用户是否已经创建了房间
        if(playerRoomMap.get(user.getStr("id"))!=null){
            //删掉原来的房间
            Room room = roomMap.get(playerRoomMap.get(user.getStr("id")));
            if(room.getPlayerId1()!=null) playerRoomMap.remove(room.getPlayerId1());
            if(room.getPlayerId2()!=null) playerRoomMap.remove(room.getPlayerId2());
            roomMap.remove(room.getId());
        }
        //创建新的房间
        Room room = new Room();
        room.setPlayerId1(user.getStr("id"));
        room.setPlayerName1(user.getStr("name"));
        //将房间保存到map并且关联上用户id
        roomMap.put(room.getId(),room);
        playerRoomMap.put(user.getStr("id"),room.getId());
        //将创建好的房间号返回给用户
        this.sendMessage(
                JSONUtil.toJsonStr(
                        new Message<>(200,"create-room-success",room)),
                session);
        System.out.println("用户:"+user.getStr("id")+"创建了新房间:"+room.getId());

        //将在线的全部房间发给全部在线用户
        Message<Collection<Room>> message = new Message<>(200,"online-rooms",roomMap.values());
        this.sendAllMessage(JSONUtil.toJsonStr(message));
    }

    private void sendMessageById(String id,String message){
        for(String key:sessionMap.keySet()){
            if(Objects.equals(key, id)){
                this.sendMessage(message,sessionMap.get(key));
                return;
            }
        }
        //如果收信人不在线，则将信息进行缓存

    }

    private void sendMessage(String message,Session toSession){
        try {
            if(toSession.isOpen())
                toSession.getBasicRemote().sendText(message);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void sendAllMessage(String message){
        try {
            for(Session session : sessionMap.values()){
                if (session.isOpen()) {
                    session.getBasicRemote().sendText(message);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
