package com.whc.fivechess.util;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;

public class MyUtil {
    public static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    //
    //public static ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    public static ByteArrayOutputStream getByteArrayOutputStream(){
        return new ByteArrayOutputStream();
    }
}
