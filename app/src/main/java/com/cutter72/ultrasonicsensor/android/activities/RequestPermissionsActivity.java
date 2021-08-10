package com.cutter72.ultrasonicsensor.android.activities;

import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.cutter72.ultrasonicsensor.R;

import java.util.Arrays;

public class RequestPermissionsActivity extends AppCompatActivity {
    public static final String PERMISSION_TYPE = "PERMISSION_TYPE";
    private static final int INITIAL_PERMISSION_ID = 1234;
    private String permissionType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_permissions);
        permissionType = getIntent().getStringExtra(PERMISSION_TYPE);
        ActivityCompat.requestPermissions(
                this,
                new String[]{permissionType},
                INITIAL_PERMISSION_ID
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        System.out.println("onRequestPermissionsResult");
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Granted.
            System.out.println("Permissions granted for: " + Arrays.toString(permissions));
        } else {
            System.out.println("Permissions denied for: " + permissionType);
        }
        onBackPressed();
    }
}