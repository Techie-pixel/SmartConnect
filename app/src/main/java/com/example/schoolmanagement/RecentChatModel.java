package com.example.schoolmanagement;

public class RecentChatModel {
    private String uid;
    private String name;
    private String lastMsg;

    public RecentChatModel(String uid, String name, String lastMsg) {
        this.uid = uid;
        this.name = name;
        this.lastMsg = lastMsg;
    }

    public String getUid() { return uid; }
    public String getName() { return name; }
    public String getLastMsg() { return lastMsg; }
}
