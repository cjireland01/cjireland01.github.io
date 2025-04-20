package com.example.projectthree;

import android.telephony.SmsManager;

public class NotificationManager {

    private SmsManager smsManager;

    public NotificationManager(SmsManager smsManager) {
        this.smsManager = smsManager;
    }

    public void notifyLowInventory(String itemName, int newQuantity, int threshold, String phoneNumber) {
        if (newQuantity < threshold) { // Checks if the updated quantity is below the threshold.
            String message = "Alert: " + itemName + " is below the threshold! Current quantity: " + newQuantity;
            sendSMS(phoneNumber, message); // Sends text message to set number if quantity is below threshold.
        }
    }

    private void sendSMS(String phoneNumber, String message) { // Sends SMS through SMSManager.
        smsManager.sendTextMessage(phoneNumber, null, message, null, null);
    }
}
