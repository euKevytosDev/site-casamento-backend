package com.casamento.backend.dto;

public class LoginResponse {

    private String token;
    private String login;
    private String siteId;
    private String role;

    public LoginResponse() {
    }

    public LoginResponse(String token, String login) {
        this(token, login, null, "ADMIN");
    }

    public LoginResponse(String token, String login, String siteId, String role) {
        this.token = token;
        this.login = login;
        this.siteId = siteId;
        this.role = role;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }

    public String getSiteId() { return siteId; }
    public void setSiteId(String siteId) { this.siteId = siteId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
