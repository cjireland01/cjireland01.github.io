package com.example.projectthree;

public class Item { // Item class for storing values of Table items.
    private String itemName;
    private int quantity;
    private String date;
    private int threshold;
    private String locationId;

    public Item() {
        // Required for Firestore deserialization
    }

    public Item(String itemName, int quantity, String date, int threshold, String locationId) {
        this.itemName = itemName;
        this.quantity = quantity;
        this.date = date;
        this.threshold = threshold;
        this.locationId = locationId;
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

    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }
}
