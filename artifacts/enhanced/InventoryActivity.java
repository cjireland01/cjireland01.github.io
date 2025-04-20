package com.example.projectthree;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * InventoryActivity manages core inventory features:
 * - CRUD operations for inventory database
 * - Sorting, filtering, searching operations
 * - Listening for Firestore item updates
 * - Sending SMS alerts based on user-set thresholds
 */
public class InventoryActivity extends AppCompatActivity {

    // UI elements
    private EditText itemNameEditText;
    private EditText itemDetailsEditText;
    private LinearLayout itemListLayout;
    private Spinner sortSpinner;
    private Spinner orderSpinner;
    private boolean isDescending = true;

    // Firebase repository and local cache
    private FirestoreInventoryRepository repository;
    private LocationManager locationManager;
    private final List<Item> itemList = new ArrayList<>();
    private final Map<String, Item> itemMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        requestSmsPermission();

        itemNameEditText = findViewById(R.id.itemNameEditText);
        itemDetailsEditText = findViewById(R.id.itemDetailsEditText);
        itemListLayout = findViewById(R.id.itemListLayout);
        sortSpinner = findViewById(R.id.sortSpinner);
        orderSpinner = findViewById(R.id.orderSpinner);
        SearchView searchView = findViewById(R.id.searchView);

        locationManager = new LocationManager(this);
        String locationId = locationManager.getStoredLocationId();

        if (locationId == null) {
            Toast.makeText(this, "Location not set. Please log in again", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        repository = new FirestoreInventoryRepository(locationId);

        Button logoutButton = findViewById(R.id.logoutButton);
        Button addItemButton = findViewById(R.id.addItemButton);
        Button updateItemButton = findViewById(R.id.updateItemButton);
        Button viewNotifsButton = findViewById(R.id.gotoNotifs);

        viewNotifsButton.setOnClickListener(v -> {
            startActivity(new Intent(this, NotificationsActivity.class));
        });

        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            finish();
        });

        addItemButton.setOnClickListener(v -> addItem());
        updateItemButton.setOnClickListener(v -> showUpdateItemDialog());

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.sort_criteria_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortSpinner.setAdapter(adapter);

        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                sortAndDisplayItems();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        ArrayAdapter<CharSequence> orderAdapter = ArrayAdapter.createFromResource(this,
                R.array.sort_order_array, android.R.layout.simple_spinner_item);
        orderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        orderSpinner.setAdapter(orderAdapter);

        orderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                isDescending = position == 1;
                sortAndDisplayItems();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterAndDisplayItems(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterAndDisplayItems(newText);
                return true;
            }
        });

        listenToInventoryChanges();
    }

    /**
     * Requests SMS permissions, if not already granted
     */
    private void requestSmsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.SEND_SMS)) {
                new AlertDialog.Builder(this)
                        .setTitle("SMS Permission Needed")
                        .setMessage("This app needs permission to send SMS so we can alert you about low inventory items.")
                        .setPositiveButton("Allow", (dialog, which) ->
                                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, 1))
                        .setNegativeButton("Deny", (dialog, which) ->
                                Toast.makeText(this, "Permission denied. SMS alerts won't work.", Toast.LENGTH_LONG).show())
                        .create()
                        .show();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, 1);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "SMS permission granted", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "SMS permission denied. You won't receive alerts.", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Listener for Firestore changes
     */
    private void listenToInventoryChanges() {
        repository.listenToItems((snapshots, error) -> {
            if (error != null || snapshots == null) return;

            itemMap.clear();
            itemList.clear();

            for (QueryDocumentSnapshot doc : snapshots) {
                Item item = doc.toObject(Item.class);
                itemMap.put(item.getItemName(), item);
                itemList.add(item);
            }

            sortAndDisplayItems();
            checkThresholdsAndNotify();
        });
    }

    /**
     * Function for adding items to database. Also adds to activity_inventory.xml layout
     */
    private void addItem() {
        String itemName = itemNameEditText.getText().toString().trim();
        String itemDetails = itemDetailsEditText.getText().toString().trim();
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        if (itemName.isEmpty() || itemDetails.isEmpty()) {
            Toast.makeText(this, "Item info is invalid", Toast.LENGTH_LONG).show();
            return;
        }

        int quantity;
        try {
            quantity = Integer.parseInt(itemDetails);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Quantity must be a number", Toast.LENGTH_SHORT).show();
            return;
        }

        repository.addItem(new Item(itemName, quantity, date, 0, locationManager.getStoredLocationId()));
        itemNameEditText.setText("");
        itemDetailsEditText.setText("");
    }


    /**
     * Sorts the list of inventory items based on selected criteria
     * Applies ascending or descending depending on user selection
     * Refreshes layout with the osrted list
     */
    private void sortAndDisplayItems() {
        String selectedSort = sortSpinner.getSelectedItem() != null ?
                sortSpinner.getSelectedItem().toString() : "Sort by Name";

        Comparator<Item> comparator;
        switch (selectedSort) {
            case "Sort by Quantity":
                comparator = Comparator.comparingInt(Item::getQuantity);
                break;
            case "Sort by Date":
                comparator = Comparator.comparing(item -> {
                    try {
                        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(item.getDate());
                    } catch (Exception e) {
                        return new Date(0);
                    }
                });
                break;
            case "Sort by Name":
            default:
                comparator = Comparator.comparing(i -> extractNaturalKey(i.getItemName()));
                break;
        }

        if (isDescending) comparator = comparator.reversed();

        Collections.sort(itemList, comparator);

        itemListLayout.removeAllViews();
        for (Item item : itemList) {
            addItemToLayout(item.getItemName(), item.getQuantity(), item.getDate());
        }
    }

    /**
     * Converts an item name containing digits into a format suitable for natural sorting
     * Ensures values like "item2" will be correctly ordered before "item10"
     *
     * @param input - The original item name
     * @return - A normalized string key with padded numeric values for sorting
     */
    private String extractNaturalKey(String input) {
        Pattern pattern = Pattern.compile("(\\D*)(\\d+)");
        Matcher matcher = pattern.matcher(input);
        if (matcher.matches()) {
            String prefix = matcher.group(1);
            int number = Integer.parseInt(matcher.group(2));
            return String.format("%s%010d", prefix, number);
        }
        return input;
    }

    /**
     * Filters the invenntory items based on search query (by name)
     * Matching items are displayed in layout, in sort order
     *
     * @param query - Search text entered by user
     */
    private void filterAndDisplayItems(String query) {
        List<Item> filteredList = new ArrayList<>();

        for (Item item : itemList) {
            if (item.getItemName().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(item);
            }
        }

        itemListLayout.removeAllViews();
        for (Item item : filteredList) {
            addItemToLayout(item.getItemName(), item.getQuantity(), item.getDate());
        }
    }

    /**
     * Dynamically creates and adds a horizontal layout row for an inventory item
     * Displays the item's name, quantity, and date, and includes a delete button
     * that removes the item from the Firestore database as well.
     *
     * @param itemName - Name of inventory item
     * @param quantity - Quantity of inventory item
     * @param date - Datestamp of item's addition/update
     */
    private void addItemToLayout(String itemName, int quantity, String date) {
        LinearLayout newItemRow = new LinearLayout(this);
        newItemRow.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        newItemRow.setOrientation(LinearLayout.HORIZONTAL);

        TextView itemNameTextView = new TextView(this);
        itemNameTextView.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        itemNameTextView.setText(itemName);

        TextView quantityTextView = new TextView(this);
        quantityTextView.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        quantityTextView.setText(String.valueOf(quantity));

        TextView dateTextView = new TextView(this);
        dateTextView.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        dateTextView.setText(date);

        Button deleteRowBtn = new Button(this);
        deleteRowBtn.setText("Delete");
        deleteRowBtn.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        deleteRowBtn.setOnClickListener(v -> repository.deleteItem(itemName));

        newItemRow.addView(itemNameTextView);
        newItemRow.addView(quantityTextView);
        newItemRow.addView(dateTextView);
        newItemRow.addView(deleteRowBtn);

        itemListLayout.addView(newItemRow);
    }

    /**
     * Displays a dialog allowing user to update the quantity of an existing item in inventory
     * Dialog validates the item exists in current inventory and that input is valid
     * then applies update via Firestore
     */
    private void showUpdateItemDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update Item");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText inputItemName = new EditText(this);
        inputItemName.setHint("Enter item name");
        layout.addView(inputItemName);

        final EditText inputQuantity = new EditText(this);
        inputQuantity.setHint("Enter new quantity");
        inputQuantity.setInputType(InputType.TYPE_CLASS_NUMBER);
        layout.addView(inputQuantity);

        builder.setView(layout);
        builder.setPositiveButton("Update", null);
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String itemName = inputItemName.getText().toString().trim();
            String newQuantityStr = inputQuantity.getText().toString().trim();

            if (itemName.isEmpty() || newQuantityStr.isEmpty()) {
                Toast.makeText(this, "Please enter both name and quantity", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!itemMap.containsKey(itemName)) {
                Toast.makeText(this, "Item not found in inventory", Toast.LENGTH_LONG).show();
                return;
            }

            int newQuantity;
            try {
                newQuantity = Integer.parseInt(newQuantityStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Quantity must be a number", Toast.LENGTH_SHORT).show();
                return;
            }

            repository.updateItemQuantity(itemName, newQuantity);
            dialog.dismiss();
        });
    }

    /**
     * Retrieves the user who set the notification's phone number and the monitored items
     * For each monitored item, compares quantity against stored threshold
     * If quantity is below threshold, send SMS alert to user's phone number
     */
    private void checkThresholdsAndNotify() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        String locationId = locationManager.getStoredLocationId();
        if (locationId == null) return;

        db.collection("locations")
                .document(locationId)
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(userDoc -> {
                    String phone = userDoc.getString("phoneNumber");
                    if (phone == null || phone.isEmpty()) {
                        return;
                    }

                    db.collection("users")
                            .document(uid)
                            .collection("notifications")
                            .get()
                            .addOnSuccessListener(snapshot -> {

                                for (DocumentSnapshot doc : snapshot) {
                                    String itemName = doc.getId();
                                    Long threshold = doc.getLong("threshold");

                                    if (threshold != null && itemMap.containsKey(itemName)) {
                                        int quantity = itemMap.get(itemName).getQuantity();

                                        if (quantity <= threshold) {
                                            sendLowInventorySMS(phone, itemName, quantity);
                                        }
                                    } else if (!itemMap.containsKey(itemName)) {
                                    }
                                }
                            })
                            .addOnFailureListener(e -> Log.e("THRESHOLD_CHECK", "Failed to load monitored notifications", e));
                })
                .addOnFailureListener(e -> Log.e("THRESHOLD_CHECK", "Failed to get user phone number", e));
    }


    /**
     * Sends a low inventory SMS alert to specified phone number using SmsManager
     * Message includes item name and current quantity
     * Displays a toast indicating success/failure of message sending
     *
     * @param phoneNumber - Recipient's phone number
     * @param itemName - Monitored item's name
     * @param quantity - Current quantity of item
     */
    private void sendLowInventorySMS(String phoneNumber, String itemName, int quantity) {
        String message = "Low inventory alert: '" + itemName + "' is now at " + quantity + " units.";

        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            Toast.makeText(this, "Alert sent for " + itemName, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to send SMS: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
