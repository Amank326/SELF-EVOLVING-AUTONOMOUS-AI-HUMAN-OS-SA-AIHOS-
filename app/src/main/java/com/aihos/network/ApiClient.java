package com.aihos.network;

/**
 * API Client for SA-AIHOS Backend Communication
 */
public class ApiClient {
    private static ApiClient instance;
    private String baseUrl;

    private ApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public static synchronized ApiClient getInstance(String baseUrl) {
        if (instance == null) {
            instance = new ApiClient(baseUrl);
        }
        return instance;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void login(String email, String password, ApiCallback callback) {
        callback.onSuccess("{ \"token\": \"test_token\" }");
    }

    public interface ApiCallback {
        void onSuccess(String response);
        void onError(String error);
    }
}

