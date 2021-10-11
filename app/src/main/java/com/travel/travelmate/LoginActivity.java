package com.travel.travelmate;

import static com.travel.travelmate.Const.UserId;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatTextView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;

public class LoginActivity extends AppCompatActivity {

    AppCompatButton btnLogin, btnSignUp, btnGoogleLogin;
    AppCompatEditText etEmail, etPassword;
    String email, password, userId;
    int userType = 0;
    SharedPreferences sharedPreferences;
    private FirebaseAuth mAuth;
    AppCompatTextView tvForgot;

    private static final String TAG = "LoginActivity";
    private static final int RC_SIGN_IN = 1234;
    private GoogleSignInClient mSignInClient;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Window mWindow = getWindow();
        mWindow.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        sharedPreferences = getSharedPreferences(Const.SHAREDPREFERENCE, MODE_PRIVATE);
        userId = sharedPreferences.getString(UserId, "0");
        if (!TextUtils.equals(userId, "0")) {

            Intent main = new Intent(LoginActivity.this, MainActivity.class);
            main.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(main);
            finish();


        }

        mAuth = FirebaseAuth.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mSignInClient = GoogleSignIn.getClient(this, gso);


        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnSignUp = findViewById(R.id.btnRegister);
        btnSignUp.setOnClickListener(v -> {
            Intent signup = new Intent(LoginActivity.this, SignUpActivity.class);
            startActivity(signup);
        });
        tvForgot = findViewById(R.id.tvForgot);
        tvForgot.setOnClickListener(v -> {
            Intent forgot = new Intent(LoginActivity.this, ForgotActivity.class);
            startActivity(forgot);
        });
        btnGoogleLogin = findViewById(R.id.btnGoogleLogin);
        btnGoogleLogin.setOnClickListener(view -> signIn());
        btnLogin = findViewById(R.id.btnLogin);
        btnLogin.setOnClickListener(v -> {

            email = etEmail.getText().toString();
            password = etPassword.getText().toString();
            if (TextUtils.isEmpty(email)) {
                etEmail.setError("This field is empty !");
                return;
            }
            if (TextUtils.isEmpty(password)) {
                etPassword.setError("This field is empty !");
                return;
            }
            DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference("users");
            mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for (DataSnapshot data : dataSnapshot.getChildren()) {

                        User user = data.getValue(User.class);

                        if (TextUtils.equals(user.getEmail(), email)) {

                            sharedPreferences = getSharedPreferences(Const.SHAREDPREFERENCE, MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString(UserId, user.getUserId());
                            editor.putString(Const.Mobile, user.getMobile());
                            editor.putString(Const.Name, user.getName());
                            editor.apply();

                            mAuth.signInWithEmailAndPassword(email, password)
                                    .addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                                        @Override
                                        public void onComplete(@NonNull Task<AuthResult> task) {
                                            if (task.isSuccessful()) {
                                                // Sign in success, update UI with the signed-in user's information
                                                //  Log.d(TAG, "signInWithEmail:success");
                                                FirebaseUser user = mAuth.getCurrentUser();
                                                Intent i = new Intent(LoginActivity.this, MainActivity.class);
                                                startActivity(i);
                                                Toast.makeText(LoginActivity.this, "Login successfully",
                                                        Toast.LENGTH_SHORT).show();
                                                finish();


                                            } else {
                                                // If sign in fails, display a message to the user.
                                                //  Log.w(TAG, "signInWithEmail:failure", task.getException());
                                                Toast.makeText(LoginActivity.this, "Authentication failed.",
                                                        Toast.LENGTH_SHORT).show();

                                            }

                                            // ...
                                        }
                                    });


                        } else {
                            Log.e("user", "not here");
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }


            });

        });
    }

    private void signIn() {
        Intent signInIntent = mSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                Log.w(TAG, "Google sign in failed", e);
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnSuccessListener(this, authResult -> {
                    DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference("users");
                    String userId = mDatabase.push().getKey();
                    User user = new User(userId, acct.getGivenName(), acct.getEmail(), "");
                    mDatabase.child(Objects.requireNonNull(userId)).setValue(user);
                    sharedPreferences = getSharedPreferences(Const.SHAREDPREFERENCE, MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString(UserId, userId);
                    editor.putString(Const.Email,acct.getEmail());
                    editor.putString(Const.Name, acct.getGivenName());
                    editor.apply();

                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(this, e -> Toast.makeText(LoginActivity.this, "Authentication failed.",
                        Toast.LENGTH_SHORT).show());
    }
}
