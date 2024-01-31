<?php

header('content-type:text/html;charset="utf-8"');

$username = $_POST['username'];
$password = $_POST['password'];
$create_time = $_POST['create_time'];

//对密码加密
$password = md5(md5($password."aaa")."bbb");

// $responddata = ["code"=>"","message"=>""];
$responddata = array("code"=>"","message"=>"");

//创建数据库

//链接数据库
$link = new mysqli("localhost","chess","532559791");

if(!$link){
    $responddata['code'] = 0;
    $responddata['message'] = "数据库链接失败";
    echo json_encode($responddata);
    exit;
}


mysqli_set_charset($connection, 'utf8');

mysqli_select_db($connection, 'test');

$sql = "SELECT * FROM users WHERE username='{$username}'";
$res1 = mysqli_query($connection, $sql1);
$row1 = mysqli_fetch_assoc($res1);

if($row){
    $responddata['code'] = 1;
    $responddata['message'] = "用户名重复";
    echo json_encode($responddata);
    exit;
}


$sql1 = "INSERT INTO users(username,password,create_time) VALUES('{$username}','{$password}',{$create_time})";
$res1 = mysqli_query($connection, $sql1);
if(!$res1){
    $responddata['code'] = 2;
    $responddata['message'] = "注册失败";
    echo json_encode($responddata);
    exit;
}else{
    $responddata['code'] = 3;
    $responddata['message'] = "注册成功";
    echo json_encode($responddata);
}

mysqli_close($connection);

?>