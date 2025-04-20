package com.example.projectthree;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RegisterActivity handles user registration using Firebase Authentication
 * and stores user info inside a sharded Firestore structure under /locations/{locationId}/users/{uid}.
 * Users must also select a location to associate their account with.
 */
public class RegisterActivity extends AppCompatActivity {

    private EditText emailEditText;
    private EditText passwordEditText;
    private EditText phoneEditText;
    private Spinner locationSpinner;
    private Button registerButton;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private LocationManager locationManager;

    private final List<String> locationIdList = new ArrayList<>();

    /**
     * Initializes Firebase, UI elements, and loads available locations.
     *
     * @param savedInstanceState - The previously saved instance state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        locationManager = new LocationManager(this);

        initializeUI();
        loadLocationsFromFirestore();
        setupRegisterButton();
    }

    /**
     * Finds and initializes UI components from the layout.
     */
    private void initializeUI() {
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        locationSpinner = findViewById(R.id.locationSpinner);
        registerButton = findViewById(R.id.registerButton);
        registerButton.setEnabled(false); // Disabled until locations are loaded
    }

    /**
     * Loads all location document IDs from Firestore under /locations
     * and populates the location spinner with them.
     */
    private void loadLocationsFromFirestore() {
        db.collection("locations")
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        locationIdList.add(doc.getId());
                    }

                    if (locationIdList.isEmpty()) {
                        Toast.makeText(this, "No locations found in database.", Toast.LENGTH_LONG).show();
                        registerButton.setEnabled(false);
                        return;
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                            android.R.layout.simple_spinner_item, locationIdList);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    locationSpinner.setAdapter(adapter);

                    registerButton.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load locations: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    registerButton.setEnabled(false);
                });
    }

    /**
     * Attaches the click listener for the register button to trigger account creation.
     */
    private void setupRegisterButton() {
        registerButton.setOnClickListener(v -> createAccount());
    }

    /**
     * Handles account creation using FirebaseAuth.
     * Validates user input, registers a new account, and stores the user under the selected location in Firestore.
     */
    private void createAccount() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();

        if (locationSpinner.getSelectedItem() == null) {
            Toast.makeText(this, "Please select a location", Toast.LENGTH_SHORT).show();
            return;
        }

        String selectedLocationId = (String) locationSpinner.getSelectedItem();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(phone)) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(selectedLocationId)) {
            Toast.makeText(this, "Select a location", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = auth.getCurrentUser();
                    if (user != null) {
                        String uid = user.getUid();

                        Map<String, Object> userInfo = new HashMap<>();
                        userInfo.put("email", email);
                        userInfo.put("phoneNumber", phone);

                        // Store user info under the selected location
                        db.collection("locations")
                                .document(selectedLocationId)
                                .collection("users")
                                .document(uid)
                                .set(userInfo)
                                .addOnSuccessListener(aVoid -> {
                                    // Fetch location and proceed to inventory
                                    locationManager.fetchAndStoreUserLocation(uid, new LocationManager.LocationCallback() {
                                        @Override
                                        public void onLocationFound(String locationId) {
                                            Toast.makeText(RegisterActivity.this, "Account created!", Toast.LENGTH_SHORT).show();
                                            startActivity(new Intent(RegisterActivity.this, InventoryActivity.class));
                                            finish();
                                        }

                                        @Override
                                        public void onError(Exception e) {
                                            Toast.makeText(RegisterActivity.this, "Error finding location: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                        }
                                    });
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Failed to store user info", Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Signup failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
