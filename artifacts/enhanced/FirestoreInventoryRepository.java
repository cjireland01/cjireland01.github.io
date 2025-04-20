package com.example.projectthree;

import android.util.Log;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * FirestoreInventoryRepository abstracts all Firestore operations
 * related to inventory items including CRUD, live updates, and threshold monitoring
 */
public class FirestoreInventoryRepository {

    private static final String TAG = "FirestoreRepo";

    private final FirebaseFirestore db;
    private final CollectionReference itemsRef;

    /**
     * Initializes Firestore instance and reference to the sharded 'inventory' collection
     * under the provided locationId
     *
     * @param locationId - the Firestore document ID for the current location
     */
    public FirestoreInventoryRepository(String locationId) {
        db = FirebaseFirestore.getInstance();
        itemsRef = db.collection("locations")
                .document(locationId)
                .collection("inventory");
    }

    /**
     * Adds a new item to Firestore or overwrites it if the item already exists
     *
     * @param item - The item to be added
     */
    public void addItem(Item item) {
        itemsRef.document(item.getItemName()).set(item)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Item added: " + item.getItemName()))
                .addOnFailureListener(e -> Log.e(TAG, "Error adding item", e));
    }

    /**
     * Updates the quantity and date of an existing item in Firestore
     *
     * @param itemName - The name of the item to update
     * @param newQuantity - The new quantity value to set
     */
    public void updateItemQuantity(String itemName, int newQuantity) {
        DocumentReference itemRef = itemsRef.document(itemName);
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        itemRef.update("quantity", newQuantity, "date", currentDate)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Item updated: " + itemName))
                .addOnFailureListener(e -> Log.e(TAG, "Error updating item", e));
    }

    /**
     * Deletes an item from Firestore by its name
     *
     * @param itemName - The name of the item to delete
     */
    public void deleteItem(String itemName) {
        itemsRef.document(itemName).delete()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Item deleted: " + itemName))
                .addOnFailureListener(e -> Log.e(TAG, "Error deleting item", e));
    }

    /**
     * Attaches a real-time listener to the 'inventory' collection
     *
     * @param listener - Firestore EventListener for snapshot updates
     */
    public void listenToItems(EventListener<QuerySnapshot> listener) {
        itemsRef.addSnapshotListener(listener);
    }
}
