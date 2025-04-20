package com.example.projectthree;

import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Collections;
import java.util.Map;

/**
 * NotificationsActivity allows the user to monitor specific items by setting quantity thresholds
 * If the item quantity falls below the threshold, an SMS will be sent to the user's phone number
 * Users can add or remove monitored items from their personal Firestore subcollection
 */
public class NotificationsActivity extends AppCompatActivity {

    private EditText itemNameEditText;
    private EditText thresholdEditText;
    private LinearLayout monitoredItemsLayout;
    private Button addMonitoredItemButton;

    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        initializeUI();
        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        loadMonitoredItems();

        addMonitoredItemButton.setOnClickListener(v -> addMonitoredItem());
    }

    /**
     * Initializes layout UI elements
     */
    private void initializeUI() {
        itemNameEditText = findViewById(R.id.itemNameEditText);
        thresholdEditText = findViewById(R.id.lowInventoryThresholdEditText);
        monitoredItemsLayout = findViewById(R.id.monitoredItemsLayout);
        addMonitoredItemButton = findViewById(R.id.addMonitoredItemButton);
    }

    /**
     * Adds a new monitored item to the user's notification list in Firestore
     * Validates for empty input and prevents duplicates
     */
    private void addMonitoredItem() {
        String itemName = itemNameEditText.getText().toString().trim();
        String thresholdStr = thresholdEditText.getText().toString().trim();

        if (itemName.isEmpty() || thresholdStr.isEmpty()) {
            Toast.makeText(this, "Please enter item name and threshold", Toast.LENGTH_SHORT).show();
            return;
        }

        int threshold;
        try {
            threshold = Integer.parseInt(thresholdStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Threshold must be a number", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(userId)
                .collection("notifications")
                .document(itemName)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Toast.makeText(this, "This item is already being monitored", Toast.LENGTH_SHORT).show();
                    } else {
                        Map<String, Object> data = Collections.singletonMap("threshold", threshold);
                        db.collection("users")
                                .document(userId)
                                .collection("notifications")
                                .document(itemName)
                                .set(data)
                                .addOnSuccessListener(unused -> {
                                    Toast.makeText(this, "Monitoring set for " + itemName, Toast.LENGTH_SHORT).show();
                                    itemNameEditText.setText("");
                                    thresholdEditText.setText("");
                                    loadMonitoredItems(); // Refresh list
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Failed to monitor item: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                });
                    }
                });
    }

    /**
     * Loads all monitored items from Firestore and displays them in the UI
     */
    private void loadMonitoredItems() {
        monitoredItemsLayout.removeAllViews();

        db.collection("users").document(userId)
                .collection("notifications")
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String itemName = doc.getId();
                        Long threshold = doc.getLong("threshold");
                        if (threshold != null) {
                            addMonitoredItemToLayout(itemName, threshold.intValue());
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load monitored items", Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * Dynamically adds a monitored item row to the layout with a remove button
     *
     * @param itemName - The name of the item being monitored
     * @param threshold - The threshold value to be monitored
     */
    private void addMonitoredItemToLayout(String itemName, int threshold) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        row.setPadding(0, 4, 0, 4);

        TextView nameView = new TextView(this);
        nameView.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 0.5f));
        nameView.setText(itemName);

        TextView thresholdView = new TextView(this);
        thresholdView.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        thresholdView.setText("Below " + threshold);
        thresholdView.setGravity(Gravity.CENTER);

        Button removeButton = new Button(this);
        removeButton.setText("Remove");
        removeButton.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        // Deletes the item from Firestore when the button is clicked
        removeButton.setOnClickListener(v -> {
            db.collection("users").document(userId)
                    .collection("notifications")
                    .document(itemName)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Removed " + itemName, Toast.LENGTH_SHORT).show();
                        loadMonitoredItems();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Error removing item", Toast.LENGTH_SHORT).show());
        });

        row.addView(nameView);
        row.addView(thresholdView);
        row.addView(removeButton);

        monitoredItemsLayout.addView(row);
    }
}
