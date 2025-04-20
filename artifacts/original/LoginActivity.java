package com.example.projectthree;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;


public class LoginActivity extends AppCompatActivity {

    private Button loginButton, registerButton;
    private EditText usernameEditText, passwordEditText;
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        loginButton = findViewById(R.id.loginButton);
        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        registerButton = findViewById(R.id.registerButton);

        databaseHelper = new DatabaseHelper(this); // Creates instance of DatabaseHelper, where all user info is stored

        loginButton.setOnClickListener(v -> {
            String username = usernameEditText.getText().toString();
            String password = passwordEditText.getText().toString();

            if (databaseHelper.checkUser(username, password)) { // Calls Databasehelper to check if user exists in the database
                // Opens Inventory screen upon successful check
                Intent intent = new Intent(LoginActivity.this, InventoryActivity.class);
                startActivity(intent);
                finish();
            }
            else {
                // If user does not exist, textboxes are not cleared, and user is warned
                Toast.makeText(LoginActivity.this, "Invalid username/password", Toast.LENGTH_SHORT).show();
            }
        });
        registerButton.setOnClickListener(v -> { // Allows user to add their username/password into the table
            String username = usernameEditText.getText().toString();
            String password = passwordEditText.getText().toString();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Please enter a username and password", Toast.LENGTH_SHORT).show();
                return;
            }

            if (databaseHelper.checkUser(username, password)) { // Makes sure duplicate entries are not added to table
                Toast.makeText(LoginActivity.this, "Username already exists", Toast.LENGTH_SHORT).show();
            }
            else {
                databaseHelper.addUser(username, password); // Creates user if all else succeeds, then logs in
                Toast.makeText(LoginActivity.this, "Registration successful", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(LoginActivity.this, InventoryActivity.class);
                startActivity(intent);
            }
        });
    }
}
