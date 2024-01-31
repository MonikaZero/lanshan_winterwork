package com.whc.fivechess.domain;



public class ResponseDto<T>{
    private boolean success = true;
    private Integer code = 200;
    private String message = "success";
    private T content;
    public ResponseDto(){}
    public ResponseDto(T t){
        this.content = t;
    }
    public ResponseDto(boolean success, Integer code, String msg){
        this.success = success;
        this.code = code;
        this.message = msg;
    }
    public void setContent(T t){
        this.content = t;
    }
    public void setSuccess(boolean success){this.success = success;}
    public void setCode(Integer code){this.code = code;}
    public void setMessage(String msg){this.message = msg;}

    public boolean getSuccess(){return this.success;}
    public Integer getCode(){return this.code;}
    public String getMessage(){return this.message;}
    public T getContent(){return this.content;}
    @Override
    public String toString(){
        return "ResponseDto{" + "success=" + success +
                ", code='" + code + '\'' +
                ", message='" + message + '\'' +
                ", content=" + content +
                '}';
    }
}
