package com.example.common.auth;
import lombok.Data;

/**
 * Abstract base class for REST transfer properties.
 */
@Data
public abstract class AbstractRestTransferProperties {
    private boolean enabled;
    private String url;
    private String username;
    private String password;
    private String certPath;
    private String certPassword;

    public boolean isEnabled() { return enabled; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getCertPath() { return certPath; }
    public void setCertPath(String certPath) { this.certPath = certPath; }

    public String getCertPassword() { return certPassword; }
    public void setCertPassword(String certPassword) { this.certPassword = certPassword; }
}
