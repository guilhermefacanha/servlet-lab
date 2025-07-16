package controller;

import dao.TenantConfigDAO;
import dao.UserDao;
import entity.TenantConfig;
import entity.User;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.io.Serializable;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.List;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import org.apache.commons.lang3.StringUtils;
import tenants.context.TenantContext;
import tenants.data.TenantConfigCache;

@Named
@SessionScoped
public class LoginController implements Serializable {

    @Inject
    private TenantConfigDAO tenantConfigDAO;

    @Inject
    private UserDao userDao;

    @Getter
    private List<TenantConfig> tenantConfigs;

    @Getter @Setter
    private String selectedTenant;
    @Getter @Setter
    private String selectedTenantName;
    @Getter @Setter
    private String username;
    @Getter @Setter
    private String password;
    @Getter @Setter
    private User loggedUser;

    @Getter
    private String serverMacAddress;

    public void loadTenantConfigs() {
        tenantConfigs = tenantConfigDAO.findAll();
    }

    public String login() {

        TenantContext.setTenantId(selectedTenant);
        // Store in session attribute
        FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("currentTenantId", selectedTenant);

        selectedTenantName = TenantConfigCache.getTenantConfig(selectedTenant).getNome();

        User user = userDao.findByUsername(username);
        if (user != null && user.getPassword().equals(hashPassword(password))) {
            loggedUser = user;
            clearFields();
            return "index?faces-redirect=true";
        } else {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Invalid credentials", "Username or password is incorrect."));
            return null;
        }
    }

    public String signOut() {
        FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
        return "login?faces-redirect=true";
    }

    public boolean isLoggedIn() {
        return loggedUser != null;
    }

    private void clearFields() {
        username = null;
        password = null;
    }

    private String hashPassword(String password) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if(hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}