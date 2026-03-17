package com.example.florist.model;

public class Category {
    private String id;
    private String name;
    private boolean isSelected;

    public Category(String id, String name) {
        this.id = id;
        this.name = name;
        this.isSelected = false;
    }

    public String getId() {return id;}
    public String getName() {return name;}
    public boolean isSelected() {return isSelected;}

    public void setSelected(boolean selected) {isSelected = selected;}




}
