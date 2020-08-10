package com.example.myapplication;


public class PhoneNumData {

    private String name;
    private String num;

    public PhoneNumData(String name, String num) {
        this.name = name;
        this.num = num;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNum() {
        return num;
    }

    public void setNum(String telPhone) {
        this.num = telPhone;
    }
}
