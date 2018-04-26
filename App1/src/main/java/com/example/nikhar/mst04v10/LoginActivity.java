package com.example.nikhar.mst04v10;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;

public class LoginActivity extends AppCompatActivity {

    Button loginButton;
    Button registerButton;
    EditText loginUsername;
    EditText loginPassword;
    File userInfo;

    public LoginActivity() {
        this.userInfo = new File(Environment.getDataDirectory().getAbsolutePath()+"/"+BuildConfig.APPLICATION_ID+"/userFolder/");
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);


        SetUIElements();
        // Example of a call to a native method

    }

    public void SetUIElements()
    {
        loginButton = this.findViewById(R.id.loginButton);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(checkCredentials()){
                    Intent recordIntent = new Intent(LoginActivity.this,RecordActivity.class);
                    startActivity(recordIntent);
                    Toast.makeText(LoginActivity.this, "Success Login", Toast.LENGTH_LONG).show();
                }
                else{
                    Toast.makeText(LoginActivity.this, "Unsuccesful Login", Toast.LENGTH_LONG).show();
                }

            }
        });

        registerButton = this.findViewById(R.id.RegisterButton);
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent registerIntent = new Intent(LoginActivity.this,UserRegisterActivity.class);
                startActivity(registerIntent);
            }
        });

        loginUsername = findViewById(R.id.LoginUsernameEditText);
        loginPassword = findViewById(R.id.LoginPasswordEditText);
    }

    private boolean checkCredentials() {
        SharedPreferences sharedPreferences = this.getSharedPreferences("Login",MODE_PRIVATE);
        String uN = sharedPreferences.getString("Username",null);
        String pW = sharedPreferences.getString("Password",null);
        if(loginUsername.getText().toString().equals(uN)&&loginPassword.getText().toString().equals(pW)){
            return true;
        }
        else{
            return false;
        }
    }


}
