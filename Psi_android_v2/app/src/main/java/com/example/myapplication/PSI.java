package com.example.myapplication;

import java.net.Socket;

public interface PSI {
    public long getDBSize();

    public void downloadDB(Socket socket);

    public boolean sendQuery(String s, Socket socket);
}
