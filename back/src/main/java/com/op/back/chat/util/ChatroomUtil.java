package com.op.back.chat.util;

public class ChatroomUtil {
    public static String createRoomId(String uid1, String uid2){
        return uid1.compareTo(uid2)<0 ? uid1+"_"+uid2 : uid2+"_"+uid1;
    }
}
