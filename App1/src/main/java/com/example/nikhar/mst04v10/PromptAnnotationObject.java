package com.example.nikhar.mst04v10;

/**
 * Created by Nikhar on 2017/10/12.
 */

public class PromptAnnotationObject {
    int wordNo;
    String word;
    Double start;
    Double end;
    String mfcc;

    public void setMfcc(String mfcc) {
        this.mfcc = mfcc;
    }

    public PromptAnnotationObject(int wordNo, String word, Double start, Double end) {
        this.wordNo = wordNo;
        this.word = word;
        this.start = start;
        this.end = end;
    }

    public Double getStart() {
        return start;
    }

    public Double getEnd() {
        return end;
    }
}
