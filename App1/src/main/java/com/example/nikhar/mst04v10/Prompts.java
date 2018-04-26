package com.example.nikhar.mst04v10;

/**
 * Created by Nikhar on 2017/09/19.
 */

public class Prompts {
    //Example prompt array
    private String [] mPromptArr = {
            "Critical equipment needs proper maintenance",
            "Addition and subtraction are learned skills",
            "Even a simple vocabulary contains symbols",
            "Cut a small corner off each edge",
            "Iguanas and alligators are tropical reptiles"
    };

    public String [] getPromptArr (){
        return this.mPromptArr;
    }

    public void setPromptArr(String [] newPromptArr){
        this.mPromptArr = newPromptArr;
    }
}
