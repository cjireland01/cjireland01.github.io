package com.example.projectthree;

public class Item { // Item class for storing values of Table items.
    private String itemName;
    private int quantity;
    private String date;
    private int lowThreshold;

    public Item(String itemName, int quantity, String date, int lowThreshold) {
        this.itemName = itemName;
        this.quantity = quantity;
        this.date = date;
        this.lowThreshold = lowThreshold;
    }

    public String getItemName() {
        return itemName;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getDate() {
        return date;
    }

    public void setQuantity(int newQuantity) {
        this.quantity = newQuantity;
    }

    public int getLowInventoryThreshold() {
        return lowThreshold;
    }
}
