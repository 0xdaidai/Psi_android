package com.example.myapplication.bloom;

import android.util.Log;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @author: kascas
 * @date: 2020/8/8-23:31
 * @description:
 */
public class BloomIO {

    public static void bloomWriter(Bloom bloom, String file) {
        Gson gson = new Gson();
        String jsonStr = gson.toJson(bloom);
        Log.d("bloomWriter: ", jsonStr);
        FileWriter fw = null;
        BufferedWriter bw = null;
        try {
            fw = new FileWriter(file);
            bw = new BufferedWriter(fw);
            bw.write(jsonStr);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bw != null)
                    bw.close();
                if (fw != null)
                    fw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static Bloom bloomReader(String file) {
        Gson gson = new Gson();
        String line = null, jsonStr;
        StringBuilder jsonStrBuilder = new StringBuilder();
        FileReader fr = null;
        BufferedReader br = null;
        try {
            fr = new FileReader(file);
            br = new BufferedReader(fr);
            while ((line = br.readLine()) != null) {
                jsonStrBuilder.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null)
                    br.close();
                if (fr != null)
                    fr.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        jsonStr = jsonStrBuilder.toString();
        Log.d("bloomReader: ", jsonStr);
        return gson.fromJson(jsonStr, Bloom.class);
    }

    public static Bloom bloomReader(File file) {
        Gson gson = new Gson();
        String line = null, jsonStr;
        StringBuilder jsonStrBuilder = new StringBuilder();
        FileReader fr = null;
        BufferedReader br = null;
        try {
            fr = new FileReader(file);
            br = new BufferedReader(fr);
            while ((line = br.readLine()) != null) {
                jsonStrBuilder.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null)
                    br.close();
                if (fr != null)
                    fr.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        jsonStr = jsonStrBuilder.toString();
        Log.d("bloomReader: ", jsonStr);
        return gson.fromJson(jsonStr, Bloom.class);
    }

}