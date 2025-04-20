package com.example.projectthree;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class NotificationActivity extends AppCompatActivity {

    private PermissionHelper permissionHelper; // Creates new instance of PermissionHelper.
    private DatabaseHelper dbHelper; // Creates new instance of DatabaseHelper.

    private LinearLayout monitoredItemsLayout;
    private EditText itemNameEditText, lowInventoryThresholdEditText;
    private Button addMonitoredItemButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        permissionHelper = new PermissionHelper(this);
        dbHelper = new DatabaseHelper(this);

        monitoredItemsLayout = findViewById(R.id.monitoredItemsLayout);
        itemNameEditText = findViewById(R.id.itemNameEditText);
        lowInventoryThresholdEditText = findViewById(R.id.lowInventoryThresholdEditText);
        addMonitoredItemButton = findViewById(R.id.addMonitoredItemButton);

        loadMonitoredItemsFromDatabase(); // Displays all currently MonitoredItems from the Table.

        addMonitoredItemButton.setOnClickListener(new View.OnClickListener() { // Checks if user has allowed SMS permissions before allowing addition of items.
            @Override
            public void onClick(View v) {
                permissionHelper.checkSmsPermission();
            }
        });
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) { // Creates Toast popup warning user if access was denied.
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionHelper.onRequestPermissionResult(requestCode, permissions, grantResults);
    }

    private void loadMonitoredItemsFromDatabase() { // Loads list of MonitoredItems from Table.
        List<Item> monitoredItems = dbHelper.getAllMonitoredItems(); // Creates list of MonitoredItems from Table.

        for (Item item : monitoredItems) { // For loop iterates through List and adds them to the Layout.
            int monitorThreshold = dbHelper.getLowInventoryThreshold(item.getItemName());
            addMonitoredItemToLayout(item.getItemName(), monitorThreshold);
        }
    }

    public void addMonitoredItem() { // Handles Database entry of new MonitoredItem.
        String itemName = itemNameEditText.getText().toString().trim();
        String thresholdStr = lowInventoryThresholdEditText.getText().toString().trim();
        int threshold = Integer.parseInt(thresholdStr);

        if (dbHelper.isItemMonitored(itemName)) {
            Toast.makeText(this, "Item is already being monitored", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!dbHelper.itemExists(itemName)) {
            Toast.makeText(this, "Item does not exist", Toast.LENGTH_SHORT).show();
            return;
        }

        dbHelper.addMonitoredItem(itemName, threshold); // Inserts the MonitoredItem into Table and Layout.
        addMonitoredItemToLayout(itemName, threshold);

        itemNameEditText.setText(""); // Clears input fields after successful submission.
        lowInventoryThresholdEditText.setText("");
    }

    private void addMonitoredItemToLayout(String itemName, int threshold) { // Adds MonitoredItems to existing Layout.
        LinearLayout itemRow = new LinearLayout(this); // Creates new "row" for MonitoredItem.
        itemRow.setOrientation(LinearLayout.HORIZONTAL);
        itemRow.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView itemNameTextView = new TextView(this); // Creates new TextView for itemName of MonitoredItem.
        itemNameTextView.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        itemNameTextView.setText(itemName);

        TextView thresholdTextView = new TextView(this); // Creates new TextView for low quantity threshold for MonitoredItem.
        thresholdTextView.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        thresholdTextView.setText(String.valueOf(threshold));

        Button deleteItemButton = new Button(this); // Creates Delete button to remove MonitoredItem from Layout and Table.
        deleteItemButton.setText("Delete");
        deleteItemButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dbHelper.deleteMonitoredItem(itemName);
                monitoredItemsLayout.removeView(itemRow);
            }
        });

        itemRow.addView(itemNameTextView);
        itemRow.addView(thresholdTextView);
        itemRow.addView(deleteItemButton);

        monitoredItemsLayout.addView(itemRow); // Adds the new Layout item.
    }

    public void onPermissionGranted() { // Can be called by PermissionHelper after permissions have been enabled to add the new MonitoredItem.
        String itemName = itemNameEditText.getText().toString().trim();
        String thresholdStr = lowInventoryThresholdEditText.getText().toString().trim();

        if (itemName.isEmpty() || thresholdStr.isEmpty()) { // Ensures that the textfields are not empty before allowing pass to addMonitoredItem().
            Toast.makeText(this, "Please enter both item name and threshold", Toast.LENGTH_SHORT).show();
            return;
        }

        addMonitoredItem();
    }
}
