package com.example.trivzclient;

public class ClientSession {
    public static String baseUrl = System.getProperty("trivz.api", "http://localhost:8080");
    public static String wsUrl   = System.getProperty("trivz.ws",  "ws://localhost:8080/ws");
    public static String token;
    public static String username;
    public static Long playerId;
    public static String role;
    public static Long currentRoomId;

    public static void setServer(String httpBase) {
        if (httpBase == null) return;
        String trimmed = httpBase.trim();
        if (trimmed.endsWith("/")) trimmed = trimmed.substring(0, trimmed.length() - 1);
        baseUrl = trimmed;
        wsUrl = trimmed.replaceFirst("^http", "ws") + "/ws";
    }
}
