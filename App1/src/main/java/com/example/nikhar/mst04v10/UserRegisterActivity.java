package com.example.nikhar.mst04v10;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class UserRegisterActivity extends AppCompatActivity {

    private EditText usernameField;
    private EditText passwordField;
    private EditText firstLanguageField;
    private EditText firstNameField;
    private EditText lastNameField;
    private EditText emailField;
    private EditText secondLanguageField;
    private EditText thirdLanguageField;
    private Button firstLanguageButton;
    private Button secondLanguageButton;
    private Button thirdLanguageButton;
    private Button registerButton;
    private User user;
    private File mfileOfUserJsonDirectory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_register);
        SetUIElements();
        SetButtonHandlers();
        
    }

    private void SetButtonHandlers() {
        firstLanguageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayLanguagePickDialog(firstLanguageField);
            }
        });
        secondLanguageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayLanguagePickDialog(secondLanguageField);
            }
        });
        thirdLanguageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayLanguagePickDialog(thirdLanguageField);
            }
        });
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveUserData();
            }
        });
    }

    private void saveUserData() {
        user = new User();
        if(checkNecessaryCredentials()){
            user.setUsername(usernameField.getText().toString());
            user.setFirstLanguage(firstLanguageField.getText().toString());
            user.setFirstName(firstNameField.getText().toString());
            user.setLastName(lastNameField.getText().toString());
            user.setEmail(emailField.getText().toString());
            user.setSecondLanguage(secondLanguageField.getText().toString());
            user.setThirdLanguage(thirdLanguageField.getText().toString());
        }
        else{
            Toast.makeText(UserRegisterActivity.this, "Invalid Credentials", Toast.LENGTH_LONG).show();
        }
        writeJsonFile(user);
        SharedPreferences sharedPreferences = getSharedPreferences("Login",MODE_PRIVATE);
        SharedPreferences.Editor Ed = sharedPreferences.edit();
        Ed.putString("Username",usernameField.getText().toString());
        Ed.putString("Password",passwordField.getText().toString());
        Ed.commit();
        Toast.makeText(UserRegisterActivity.this, "Successful Registration", Toast.LENGTH_LONG).show();
    }


    public void writeJsonFile(User user)
    {
        mfileOfUserJsonDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/"+BuildConfig.APPLICATION_ID+"/jsonUserFolder/");
        mfileOfUserJsonDirectory.mkdirs();
        Gson gson = new Gson();
        String s = gson.toJson(user,User.class);
        FileWriter file = null;
        try {
            file = new FileWriter(mfileOfUserJsonDirectory+"/"+user.getUsername()+".json");
            file.write(s);
            file.flush();
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //Toast.makeText(this,mfileOfJsonDirectory+"/"+mPromptNo+"clip.json", Toast.LENGTH_LONG).show();

    }

    private boolean checkNecessaryCredentials() {
        if(usernameField.getText().toString().equals(null)&&
                passwordField.getText().toString().equals(null)&&
                firstLanguageField.getText().toString().equals(null)){
            return false;
        }
        else{
            return true;
        }
    }

    private void displayLanguagePickDialog(final EditText editText) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pick a language");
        final String[] selection = new String[1];
// add a radio button list
        final String[] languages = getResources().getStringArray(R.array.language_names);
        final int[] checkedItem = {1}; // cow
        builder.setSingleChoiceItems(languages, checkedItem[0], new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // user checked an item
            checkedItem[0] = which;
            }
        });

// add OK and Cancel buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // user clicked OK
                editText.setText(languages[checkedItem[0]]);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

// create and show the alert dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void SetUIElements() {

        usernameField = findViewById(R.id.UsernameEditText);
        passwordField = findViewById(R.id.PasswordEditText);
        firstLanguageField = findViewById(R.id.FirstLanguageEditText);
        firstNameField = findViewById(R.id.FirstNameEditText);
        lastNameField = findViewById(R.id.LastNameEditText);
        emailField = findViewById(R.id.EmailEditText);
        secondLanguageField = findViewById(R.id.SecondLanguageEditText);
        thirdLanguageField = findViewById(R.id.ThirdLanguageEditText);
        firstLanguageButton = findViewById(R.id.FirstLanguagePickButton);
        secondLanguageButton = findViewById(R.id.SecondLanguagePickButton);
        thirdLanguageButton = findViewById(R.id.ThirdLanguagePickButton);
        registerButton =findViewById(R.id.RegisterButton);
    }

}
