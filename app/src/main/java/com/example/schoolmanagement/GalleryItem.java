package com.example.schoolmanagement;

public class GalleryItem {
    private String image; // Base64 string
    private String description;

    public GalleryItem() {
        // Empty constructor needed for Firebase
    }

    public GalleryItem(String image, String description) {
        this.image = image;
        this.description = description;
    }

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

    // Check if description is valid
    public boolean hasDescription() {
        return description != null && !description.trim().isEmpty();
    }
}