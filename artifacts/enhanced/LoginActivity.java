package com.example.projectthree;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * LoginActivity handles user authentication using FirebaseAuth.
 * Provides login functionality and allows users to navigate to the registration screen.
 * If the user is an admin, they are directed to the AdminActivity;
 * otherwise, they are directed to the InventoryActivity based on their location data.
 */
public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private Button registerButton;

    private FirebaseAuth auth;
    private LocationManager locationManager;

    /**
     * Initializes the activity, FirebaseAuth, UI elements, and listeners.
     * Also checks for an already authenticated user to auto-login.
     *
     * @param savedInstanceState - Saved state of the activity
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initializeFirebaseAuth();
        initializeUIElements();
        locationManager = new LocationManager(this);

        checkAutoLogin();
        setupListeners();
    }

    /**
     * Initializes Firebase authentication instance.
     */
    private void initializeFirebaseAuth() {
        auth = FirebaseAuth.getInstance();
    }

    /**
     * Finds and assigns UI components from the layout.
     */
    private void initializeUIElements() {
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);
    }

    /**
     * If a user is already logged in, proceed to fetch their location and open InventoryActivity.
     */
    private void checkAutoLogin() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            fetchAndGoToInventory(user.getUid());
        }
    }

    /**
     * Sets click listeners for login and register buttons.
     */
    private void setupListeners() {
        loginButton.setOnClickListener(v -> loginUser());
        registerButton.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }

    /**
     * Attempts to log the user in with the provided email and password.
     * Checks if the user is an admin or a regular user and navigates accordingly.
     */
    private void loginUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = auth.getCurrentUser();
                    if (user == null) return;

                    String uid = user.getUid();

                    // Check if the user is an admin
                    FirebaseFirestore.getInstance()
                            .collection("admins")
                            .document(uid)
                            .get()
                            .addOnSuccessListener(doc -> {
                                if (doc.exists()) {
                                    Toast.makeText(this, "Admin login successful", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(this, AdminActivity.class));
                                    finish();
                                } else {
                                    fetchAndGoToInventory(uid);
                                }
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to check admin role: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Login failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Fetches the user's associated location and navigates to InventoryActivity upon success.
     *
     * @param uid - The user's unique ID
     */
    private void fetchAndGoToInventory(String uid) {
        locationManager.fetchAndStoreUserLocation(uid, new LocationManager.LocationCallback() {
            @Override
            public void onLocationFound(String locationId) {
                Toast.makeText(LoginActivity.this, "Location detected: " + locationId, Toast.LENGTH_SHORT).show();
                navigateToInventory();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(LoginActivity.this, "Failed to find location: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Launches the InventoryActivity and closes the current login screen.
     */
    private void navigateToInventory() {
        startActivity(new Intent(this, InventoryActivity.class));
        finish();
    }
}
