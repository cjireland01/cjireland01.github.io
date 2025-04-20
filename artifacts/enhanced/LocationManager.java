package com.example.projectthree;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LocationManager {

    private static final String TAG = "LocationManager";
    private static final String PREFS_NAME = "AppPrefs";
    private static final String LOCATION_KEY = "locationId";

    private final FirebaseFirestore db;
    private final Context context;

    public LocationManager(Context context) {
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
    }

    public interface LocationCallback {
        void onLocationFound(String locationId);
        void onError(Exception e);
    }

    /**
     * Attempts to find the user's locationId by checking all location collections.
     * Stores the locationId in SharedPreferences if found.
     */
    public void fetchAndStoreUserLocation(@NonNull String userId, @NonNull LocationCallback callback) {
        db.collection("locations")
                .get()
                .addOnSuccessListener(locationSnapshots -> {
                    for (DocumentSnapshot locationDoc : locationSnapshots.getDocuments()) {
                        String locationId = locationDoc.getId();

                        DocumentReference userRef = db.collection("locations")
                                .document(locationId)
                                .collection("users")
                                .document(userId);

                        userRef.get().addOnSuccessListener(userDoc -> {
                            if (userDoc.exists()) {
                                // Store locationId
                                storeLocationId(locationId);
                                callback.onLocationFound(locationId);
                            }
                        });
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    /**
     * Stores the locationId in SharedPreferences
     */
    private void storeLocationId(String locationId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(LOCATION_KEY, locationId).apply();
        Log.d(TAG, "Stored locationId: " + locationId);
    }

    /**
     * Retrieves the stored locationId from SharedPreferences
     */
    public String getStoredLocationId() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(LOCATION_KEY, null);
    }

    /**
     * Clears stored locationId (e.g., on logout)
     */
    public void clearStoredLocationId() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(LOCATION_KEY).apply();
        Log.d(TAG, "Cleared locationId from preferences");
    }
}
