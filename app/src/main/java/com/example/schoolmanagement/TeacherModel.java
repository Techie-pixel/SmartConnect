package com.example.schoolmanagement;

public class TeacherModel {
    private String id;
    private String name;
    private String standard;
    private String stream;
    private String subject;
    private String profileBase64;

    public TeacherModel() { }

    public TeacherModel(String id, String name, String standard,
                        String stream, String subject, String profileBase64) {
        this.id = id;
        this.name = name;
        this.standard = standard;
        this.stream = stream;
        this.subject = subject;
        this.profileBase64 = profileBase64;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getStandard() { return standard; }
    public String getStream() { return stream; }
    public String getSubject() { return subject; }
    public String getProfileBase64() { return profileBase64; }
}
