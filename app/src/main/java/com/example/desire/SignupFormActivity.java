package com.example.desire;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SignupFormActivity extends AppCompatActivity {

    private EditText etName, etUsername, etBirthday, etPassword, etConfirmPassword;
    private Button btnContinue;
    private CheckBox cbTerms;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private String emailText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signupform);

        emailText = getIntent().getStringExtra("emailText");

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        // Initialize Firebase Database
        mDatabase = FirebaseDatabase.getInstance().getReference().child("users");

        etName = findViewById(R.id.et_name);
        etUsername = findViewById(R.id.et_username);
        etBirthday = findViewById(R.id.et_birthday);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        cbTerms = findViewById(R.id.cb_terms);
        btnContinue = findViewById(R.id.btn_continue);

        btnContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signUp();
            }
        });
    }

    private void signUp() {
        final String name = etName.getText().toString().trim();
        final String username = etUsername.getText().toString().trim();
        final String birthday = etBirthday.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        boolean termsChecked = cbTerms.isChecked();

        // Validate input fields
        if (name.isEmpty()) {
            etName.setError("Name is required");
            etName.requestFocus();
            return;
        }

        if (username.isEmpty()) {
            etUsername.setError("Username is required");
            etUsername.requestFocus();
            return;
        }

        if (birthday.isEmpty()) {
            etBirthday.setError("Birthday is required");
            etBirthday.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }

        if (confirmPassword.isEmpty()) {
            etConfirmPassword.setError("Confirm Password is required");
            etConfirmPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return;
        }

        if (!termsChecked) {
            Toast.makeText(this, "You must agree to the terms and conditions", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a new user in Firebase Authentication
        mAuth.createUserWithEmailAndPassword(emailText, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign up success
                            FirebaseUser firebaseUser = mAuth.getCurrentUser();
                            if (firebaseUser != null) {
                                // Get the user ID from Firebase Authentication
                                String userId = firebaseUser.getUid();

                                // Create User object
                                User user = new User(userId, emailText, name, username, birthday);

                                // Save the User object to Firebase Realtime Database
                                mDatabase.child(userId).setValue(user)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()) {
                                                    // Data saved successfully
                                                    Toast.makeText(SignupFormActivity.this, "Sign up successful!", Toast.LENGTH_SHORT).show();
                                                    Intent intent = new Intent(SignupFormActivity.this, ProfileActivity.class);
                                                    startActivity(intent);
                                                    finish(); // Optional, if you want to remove this activity from the stack
                                                } else {
                                                    // Handle failure
                                                    Toast.makeText(SignupFormActivity.this, "Failed to store user data.", Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        });
                            }
                        } else {
                            // Sign up failed
                            Log.e(SignupFormActivity.class.getSimpleName(), "Sign up failed: " + task.getException().getMessage());
                            if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                                Toast.makeText(SignupFormActivity.this, "The email address is already in use by another account.", Toast.LENGTH_SHORT).show();
                                // Redirect to login activity if needed
                                Intent intent = new Intent(SignupFormActivity.this, LoginActivity.class);
                                intent.putExtra("emailText", emailText);
                                startActivity(intent);
                                finish(); // Optional, if you want to remove this activity from the stack
                            } else {
                                Toast.makeText(SignupFormActivity.this, "Sign up failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
    }

    // User class to model user data
    public static class User {
        public String userId;
        public String email;
        public String name;
        public String username;
        public String birthday;

        public User() {
            // Default constructor required for calls to DataSnapshot.getValue(User.class)
        }

        public User(String userId, String email, String name, String username, String birthday) {
            this.userId = userId;
            this.email = email;
            this.name = name;
            this.username = username;
            this.birthday = birthday;
        }
    }
}
