package com.example.projectthree;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.List;

public class InventoryActivity extends AppCompatActivity {

    private EditText itemNameEditText;
    private EditText itemDetailsEditText;
    private LinearLayout itemListLayout;

    private DatabaseHelper dbHelper;

    private final String phoneNumber = "555-123-4567"; // SETS USER PHONE NUMBER.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        itemNameEditText = findViewById(R.id.itemNameEditText);
        itemDetailsEditText = findViewById(R.id.itemDetailsEditText);
        itemListLayout = findViewById(R.id.itemListLayout);

        dbHelper = new DatabaseHelper(this); // Creates instance of DatabaseHelper

        Button addItemButton = findViewById(R.id.addItemButton);
        Button updateItemButton = findViewById(R.id.updateItemButton);
        Button viewNotifsButton = findViewById(R.id.gotoNotifs);

        addItemButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addItem();
            }
        });

        updateItemButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showUpdateItemDialog();
            }
        });

        viewNotifsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(InventoryActivity.this, NotificationActivity.class);
                startActivity(intent);
            }

        });

        loadItemsFromDatabase(); // Displays all information in the Item table within DatabaseHelper upon creation of Activity
    }

    private void addItem() {
        String itemName = itemNameEditText.getText().toString().trim();
        String itemDetails = itemDetailsEditText.getText().toString().trim();
        int quantity;
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        if (itemName.isEmpty() || itemDetails.isEmpty()) {
            Toast.makeText(InventoryActivity.this, "Item info is invalid", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            quantity = Integer.parseInt(itemDetails);
        } catch (NumberFormatException e) {
            return;
        }

        if (!dbHelper.itemExists(itemName)) { // If item does not exist yet, adds item to table and layout for instant viewing, then clears textfields.
            dbHelper.addItem(itemName, quantity, date);

            itemNameEditText.setText("");
            itemDetailsEditText.setText("");

            addItemToLayout(itemName, quantity, date);
        }
        else {
            Toast.makeText(InventoryActivity.this, "This item already exists", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadItemsFromDatabase() {
        List<Item> items = dbHelper.getAllItems();
        for (Item item : items) { // Calls addItemToLayout() in a for loop until all items from Table are in the layout.
            addItemToLayout(item.getItemName(), item.getQuantity(), item.getDate());
        }
    }

    private void addItemToLayout(String itemName, int quantity, String date) {

        LinearLayout newItemRow = new LinearLayout(this); // Modifies the runtime layout to fit new items.

        newItemRow.setLayoutParams(new LinearLayout.LayoutParams( // Creates new "row" for items.
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        newItemRow.setOrientation(LinearLayout.HORIZONTAL);

        TextView itemNameTextView = new TextView(this); // Creates new TextView for itemName.
        itemNameTextView.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        itemNameTextView.setText(itemName);

        TextView quantityTextView = new TextView(this); // Creates new TextView for quantity.
        quantityTextView.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        quantityTextView.setText(String.valueOf(quantity));

        TextView dateTextView = new TextView(this); // Creates new TextView for lastModified date.
        dateTextView.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        dateTextView.setText(date);

        Button deleteRowBtn = new Button(this); // Creates a delete Button for deletion of items from layout and Table.
        deleteRowBtn.setText("Delete");
        deleteRowBtn.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        deleteRowBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { // Deletes item associated with itemName from Table and erases from Layout for instant viewing.
                dbHelper.deleteItem(itemName);
                itemListLayout.removeView(newItemRow);
            }
        });

        newItemRow.addView(itemNameTextView);
        newItemRow.addView(quantityTextView);
        newItemRow.addView(dateTextView);
        newItemRow.addView(deleteRowBtn);

        itemListLayout.addView(newItemRow); // Adds new LinearLayout to existing "frame" in activity_inventory.xml.
    }

    private void showUpdateItemDialog() { // Creates popup to allow user to modify existing item quantities.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update Item");

        LinearLayout layout = new LinearLayout(this); // Creates small popup LinearLayout.
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText inputItemName = new EditText(this); // Generates itemName EditText in popup.
        inputItemName.setHint("Enter item name");
        layout.addView(inputItemName);

        final EditText inputQuantity = new EditText(this); // Generates quantity EditText in popup.
        inputQuantity.setHint("Enter new quantity");
        inputQuantity.setInputType(InputType.TYPE_CLASS_NUMBER);
        layout.addView(inputQuantity);

        builder.setView(layout); // Sets the view of the Alert Dialog builder.

        builder.setPositiveButton("Update", new DialogInterface.OnClickListener() { // Handles the "yes"/positive button for the Alert, handling modification of item quantity.
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String itemName = inputItemName.getText().toString().trim();
                String newQuantityStr = inputQuantity.getText().toString().trim();

                if (!itemName.isEmpty() && !newQuantityStr.isEmpty()) {
                    int newQuantity = Integer.parseInt(newQuantityStr);
                    dbHelper.updateItem(itemName, newQuantity, phoneNumber);
                    itemListLayout.removeAllViews();
                    loadItemsFromDatabase(); // Upon completion, reload layout with new information.
                } else {
                    Toast.makeText(getApplicationContext(), "Please enter valid input", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() { // If "no"/negative button is pressed, cancel dialog.
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show(); // Display Alert dialog.
    }
}
