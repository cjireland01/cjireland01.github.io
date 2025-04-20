package com.example.projectthree;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * AdminActivity allows administrators to view inventory across all warehouse locations.
 * Admins can select a location from a spinner and view its inventory in a RecyclerView.
 */
public class AdminActivity extends AppCompatActivity {

    private Spinner locationSpinner;
    private RecyclerView inventoryRecyclerView;

    private FirebaseFirestore db;
    private final List<String> locationIds = new ArrayList<>();
    private final List<Item> inventoryItems = new ArrayList<>();
    private InventoryAdapter adapter;

    /**
     * Initializes the admin interface, loads all available warehouse locations,
     * and sets up inventory view behavior based on location selection.
     *
     * @param savedInstanceState - The previously saved instance state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        db = FirebaseFirestore.getInstance();

        locationSpinner = findViewById(R.id.locationSpinner);
        inventoryRecyclerView = findViewById(R.id.inventoryRecyclerView);

        adapter = new InventoryAdapter(inventoryItems);
        inventoryRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        inventoryRecyclerView.setAdapter(adapter);

        loadLocations();
        setupLocationSelectionListener();
    }

    /**
     * Loads all warehouse location IDs from the /locations collection in Firestore
     * and populates the spinner with those location IDs.
     */
    private void loadLocations() {
        db.collection("locations")
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        locationIds.add(doc.getId());
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this, android.R.layout.simple_spinner_item, locationIds);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    locationSpinner.setAdapter(adapter);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load locations", Toast.LENGTH_SHORT).show());
    }

    /**
     * Sets a listener on the location spinner to detect selection changes.
     * Loads the corresponding inventory data when a new location is selected.
     */
    private void setupLocationSelectionListener() {
        locationSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                String selectedLocation = locationIds.get(position);
                loadInventoryForLocation(selectedLocation);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    /**
     * Loads inventory data for the selected location from Firestore.
     * Updates the RecyclerView with a fresh list of inventory items.
     *
     * @param locationId - The Firestore document ID for the selected location
     */
    private void loadInventoryForLocation(String locationId) {
        db.collection("locations")
                .document(locationId)
                .collection("inventory")
                .get()
                .addOnSuccessListener(snapshot -> {
                    inventoryItems.clear();
                    for (DocumentSnapshot doc : snapshot) {
                        Item item = doc.toObject(Item.class);
                        if (item != null) {
                            item.setLocationId(locationId); // Set location reference in the item
                            inventoryItems.add(item);
                        }
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load inventory", Toast.LENGTH_SHORT).show());
    }
}
