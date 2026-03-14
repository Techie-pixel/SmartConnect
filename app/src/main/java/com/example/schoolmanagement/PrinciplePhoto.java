package com.example.schoolmanagement;

public class PrinciplePhoto {
    public String image;
    public String description;

    // Empty constructor required for Firebase
    public PrinciplePhoto() {
    }

    public PrinciplePhoto(String image, String description) {
        this.image = image;
        this.description = description;
    }

    // Getters and Setters (optional but good practice)
    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}