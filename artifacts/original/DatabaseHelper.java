package com.example.projectthree;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.telephony.SmsManager;
import android.util.Log;


public class DatabaseHelper extends SQLiteOpenHelper { // Database Manager for use with SQLite DB.

    private static final String DATABASE_NAME = "InventoryManager.db"; // Sets database name within files.
    private static final int DATABASE_VERSION = 1; // Sets version, for possible future update checks.

    private static final String USER_TABLE = "Users"; // Sets up Table of Usernames and Passwords.
    private static final String COLUMN_USERNAME = "username";
    private static final String COLUMN_PASSWORD = "password";

    private static final String ITEM_TABLE = "Items"; // Sets up Table of Items, including information for Notifications.
    private static final String COLUMN_ITEM_NAME = "itemName";
    private static final String COLUMN_QUANTITY = "quantity";
    private static final String COLUMN_DATE_MODIFIED = "dateModified";
    private static final String COLUMN_MONITORED_THRESHOLD = "threshold";
    private static final String COLUMN_IS_MONITORED = "isMonitored";

    // Creates User Table.
    private static final String CREATE_USER_TABLE =
            "CREATE TABLE " + USER_TABLE + " (" +
                    COLUMN_USERNAME + " TEXT PRIMARY KEY, " +
                    COLUMN_PASSWORD + " TEXT NOT NULL);";

    // Creates Items Table.
    private static final String CREATE_ITEM_TABLE =
            "CREATE TABLE " + ITEM_TABLE + " (" +
                    COLUMN_ITEM_NAME + " TEXT PRIMARY KEY, " +
                    COLUMN_QUANTITY + " INTEGER NOT NULL, " +
                    COLUMN_DATE_MODIFIED + " TEXT NOT NULL, " +
                    COLUMN_IS_MONITORED + " INTEGER DEFAULT 0, " +
                    COLUMN_MONITORED_THRESHOLD +" INTEGER DEFAULT 0);";

    public DatabaseHelper(Context context) { // Creates accessible DatabaseHelper for later use.
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_USER_TABLE);
        db.execSQL(CREATE_ITEM_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { // If database is updated, drops previous Tables.
        db.execSQL("DROP TABLE IF EXISTS " + USER_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + ITEM_TABLE);
        onCreate(db);
    }

    public void addUser(String username, String password) { // Inserts a newly registered User to Table.
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, username);
        values.put(COLUMN_PASSWORD, password);
        db.insert(USER_TABLE, null, values);
        db.close();
    }

    public boolean checkUser(String username, String password) { // Checks if User exists as boolean.
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(USER_TABLE, new String[]{COLUMN_USERNAME},
                COLUMN_USERNAME + "=? AND " + COLUMN_PASSWORD + "=?",
                new String[]{username, password}, null, null, null);
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return exists;
    }

    public void addItem(String itemName, int quantity, String dateModified) { // Inserts newly created Item into Items Table.
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_ITEM_NAME, itemName);
        values.put(COLUMN_QUANTITY, quantity);
        values.put(COLUMN_DATE_MODIFIED, dateModified);

        long result = db.insert(ITEM_TABLE, null, values); // Inserts item, returns number for check.
        if (result == -1) {
            Log.d("DatabaseHelper", "Failed to insert");
        }
        else {
            Log.d("DatabaseHelper", "Inserted row");
        }
        db.close();
    }

    public List<Item> getAllItems() { // Returns a List of Items from Table in Database.
        List<Item> itemList = new ArrayList<>(); // Creates new List.

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(ITEM_TABLE, null, null, null, null, null, null); // Creates Cursor for querying.

        if (cursor.moveToFirst()) {
            do { // Until there are no more items, add values to ItemList Items.
                @SuppressLint("Range") String itemName = cursor.getString(cursor.getColumnIndex(COLUMN_ITEM_NAME));
                @SuppressLint("Range") int quantity = cursor.getInt(cursor.getColumnIndex(COLUMN_QUANTITY));
                @SuppressLint("Range") String date = cursor.getString(cursor.getColumnIndex(COLUMN_DATE_MODIFIED));
                @SuppressLint("Range") int lowInventoryThreshold = cursor.getInt(cursor.getColumnIndex(COLUMN_MONITORED_THRESHOLD));

                Item item = new Item(itemName, quantity, date, lowInventoryThreshold);
                itemList.add(item);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return itemList;
    }

    public void deleteItem(String itemName) { // Erases items from Database upon call based upon the Item's name.
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("Items", "itemName = ?", new String[]{itemName});
        db.close();
    }

    public boolean itemExists(String itemName) { // Checks whether the item already exists in the Table.
        SQLiteDatabase db = this.getWritableDatabase();

        String query = "SELECT * FROM Items WHERE itemName = ?";
        Cursor cursor = db.rawQuery(query, new String[]{itemName});

        boolean exists = (cursor.getCount() > 0);
        cursor.close();
        db.close();
        return exists;
    }

    public void updateItem(String itemName, int newQuantity, String phoneNumber) { // Updates entries of Items in Database.
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_QUANTITY, newQuantity); // Sets new Quantity from User Input

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String currentDate = sdf.format(new Date());
        values.put(COLUMN_DATE_MODIFIED, currentDate); // Sets date of Last Modified to current Date

        db.update(ITEM_TABLE, values, COLUMN_ITEM_NAME + " = ?", new String[]{itemName});

        SmsManager smsManager = SmsManager.getDefault();
        NotificationManager notificationManager = new NotificationManager(smsManager);
        int threshold = getLowInventoryThreshold(itemName);
        notificationManager.notifyLowInventory(itemName, newQuantity, threshold, phoneNumber); // Sends new Quantity of Item to NotificationManager to check if it should notify user.

        db.close();
    }

    public void addMonitoredItem(String itemName, int threshold) { // Sets Item isMonitored value from 0 to 1 to add to MonitoredItems "list".
        SQLiteDatabase db = this.getWritableDatabase();

        Cursor cursor = db.query(ITEM_TABLE, null, COLUMN_ITEM_NAME + "=?", new String[]{itemName}, null, null, null); // Creates Cursor for found item.

        ContentValues values = new ContentValues();
        values.put(COLUMN_IS_MONITORED, 1); // Sets isMonitored to 1(true) from 0(false).
        values.put(COLUMN_MONITORED_THRESHOLD, threshold); // Adds the low inventory threshold.

        db.update(ITEM_TABLE, values, COLUMN_ITEM_NAME + "=?", new String[]{itemName}); // Updates the item in the Items table.

        cursor.close();
        db.close();
    }

    public List<Item> getAllMonitoredItems() { // Populates list with all MonitoredItems.
        List<Item> monitoredItems = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(ITEM_TABLE, null, COLUMN_IS_MONITORED + "=?", new String[]{"1"}, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                @SuppressLint("Range") String itemName = cursor.getString(cursor.getColumnIndex(COLUMN_ITEM_NAME));
                @SuppressLint("Range") int quantity = cursor.getInt(cursor.getColumnIndex(COLUMN_QUANTITY));
                @SuppressLint("Range") int threshold = cursor.getInt(cursor.getColumnIndex(COLUMN_MONITORED_THRESHOLD));
                @SuppressLint("Range") String date = cursor.getString(cursor.getColumnIndex(COLUMN_DATE_MODIFIED));
                monitoredItems.add(new Item(itemName, quantity, date, threshold));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return monitoredItems; // Returns List of MonitoredItems, Items where isMonitored is set to 1.
    }

    public boolean isItemMonitored(String itemName) { // Checks if Item is already Monitored.
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(ITEM_TABLE, null, COLUMN_ITEM_NAME + "=? AND " + COLUMN_IS_MONITORED + "=?", new String[]{itemName, "1"}, null, null, null);
        boolean exists = (cursor.getCount() > 0);
        cursor.close();
        db.close();
        return exists; // Returns true/false based upon results of query.
    }

    @SuppressLint("Range")
    public int getLowInventoryThreshold(String itemName) { // Gets the low inventory threshold of each item passed to it.
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COLUMN_MONITORED_THRESHOLD + " FROM " + ITEM_TABLE + " WHERE " + COLUMN_ITEM_NAME + " = ? AND " + COLUMN_IS_MONITORED + "=?", new String[]{itemName, "1"});

        int threshold = -1;

        if (cursor.moveToFirst()) {
            threshold = cursor.getInt(cursor.getColumnIndex(COLUMN_MONITORED_THRESHOLD));
        }
        cursor.close();
        db.close();
        return threshold; // Returns value of threshold.
    }

    public void deleteMonitoredItem(String itemName) { // Deletes MonitoredItem.
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_IS_MONITORED, 0); // Sets isMonitored to 0(false) on the Item, as there is no separate Table.
        db.update(ITEM_TABLE, values, COLUMN_ITEM_NAME + "=?", new String[]{itemName});
        db.close();
    }
}
