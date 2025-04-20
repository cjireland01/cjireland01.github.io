package com.example.projectthree;

import android.Manifest;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class PermissionHelper { // Enables SMS Permissions for use by NotificationActivity.
    private static final int SMS_PERMISSION_REQUEST_CODE = 1; // Keeps track of Permission Code for unique identification.
    private NotificationActivity activity; // Loads NotificationActivity for calling methods.

    public PermissionHelper(NotificationActivity activity) {
        this.activity = activity;
    }

    public void checkSmsPermission() { // Checks if user has granted permission for SMS usage on first time.
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) { // Checks if user has granted SMS Permissions.
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_REQUEST_CODE); // If not, request permission.
        } else { // Otherwise, proceed to NotificationActivity onPermissionGranted().
            activity.onPermissionGranted();
        }
    }

    public void onRequestPermissionResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) { // Checks if user has allowed app to send SMS on Notifications.
        if (requestCode == SMS_PERMISSION_REQUEST_CODE) { // Makes sure requestCode matches unique identifier "1".
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) { // If permissions are granted, calls NotificationActivity onPermissionGranted().
                activity.onPermissionGranted();
            } else { // Otherwise, warn user.
                Toast.makeText(activity, "SMS permission is required to send notifications", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
