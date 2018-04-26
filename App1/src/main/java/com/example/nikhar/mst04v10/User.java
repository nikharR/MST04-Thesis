package com.example.nikhar.mst04v10;


/**
 * Created by Nikhar on 2017/11/08.
 */

public class User {


    String firstNameKey = "first_name";
    String lastNameKey = "last_name";
    String emailKey = "email";
    String firstLanguageKey = "first_language";
    String secondLanguageKey = "second_language";
    String thirdLanguageKey = "third_language";

    private int id;



    private String username = null;
    private String firstName = null;
    private String lastName = null;
    private String email = null;
    private String firstLanguage = null;
    private String secondLanguage = null;
    private String thirdLanguage = null;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstLanguage() {
        return firstLanguage;
    }

    public void setFirstLanguage(String firstLanguage) {
        this.firstLanguage = firstLanguage;
    }

    public String getSecondLanguage() {
        return secondLanguage;
    }

    public void setSecondLanguage(String secondLanguage) {
        this.secondLanguage = secondLanguage;
    }

    public String getThirdLanguage() {
        return thirdLanguage;
    }

    public void setThirdLanguage(String thirdLanguage) {
        this.thirdLanguage = thirdLanguage;
    }




}
