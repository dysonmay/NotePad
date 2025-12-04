package com.example.android.notepad;

public class Tag {
    private long id;
    private String name;
    private int color;
    private long createdDate;

    public Tag() {}

    public Tag(long id, String name, int color) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.createdDate = System.currentTimeMillis();
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getColor() { return color; }
    public void setColor(int color) { this.color = color; }

    public long getCreatedDate() { return createdDate; }
    public void setCreatedDate(long createdDate) { this.createdDate = createdDate; }

    @Override
    public String toString() {
        return name;
    }
}