package com.example.schoolmanagement;

public class StudentModel {
    private String uid; // unique id for chat/profile
    private String name;
    private String standard;
    private String stream;
    private String profileImageBase64; // base64 string

    public StudentModel() {}

    public StudentModel(String uid, String name, String standard, String stream, String profileImageBase64) {
        this.uid = uid;
        this.name = name;
        this.standard = standard;
        this.stream = stream;
        this.profileImageBase64 = profileImageBase64;
    }

    public String getUid() { return uid; }
    public String getName() { return name; }
    public String getStandard() { return standard; }
    public String getStream() { return stream; }
    public String getProfileImageBase64() { return profileImageBase64; }
}
