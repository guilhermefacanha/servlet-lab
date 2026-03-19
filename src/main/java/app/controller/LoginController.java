package app.controller;

import app.dao.TenantConfigDAO;
import app.dao.UserDao;
import app.entity.TenantConfig;
import app.entity.User;
import app.tenants.resolver.MyTenantIdentifierResolver;
import lombok.Getter;
import lombok.Setter;
import app.tenants.context.TenantContext;
import app.tenants.data.TenantConfigCache;
import org.apache.commons.lang3.StringUtils;

import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.List;

@Named
@SessionScoped
public class LoginController implements Serializable {

    @Inject
    private TenantConfigDAO tenantConfigDAO;

    @Inject
    private UserDao userDao;

    @Getter
    private List<TenantConfig> tenantConfigs;

    @Getter
    @Setter
    private String selectedTenant;
    @Getter
    @Setter
    private String selectedTenantName;
    @Getter
    @Setter
    private String username;
    @Getter
    @Setter
    private String password;
    @Getter
    @Setter
    private User loggedUser;

    @Getter
    private String serverMacAddress;

    private boolean currentTenantSessionCreated = false;

    public void loadTenantConfigs() {
        TenantContext.setTenantId(MyTenantIdentifierResolver.DEFAULT_TENANT_ID);
        tenantConfigs = tenantConfigDAO.findAll();
        TenantContext.clear();
    }

    public void checkTenantSession() {
        if (StringUtils.isBlank(selectedTenant)) {
            if (FacesContext.getCurrentInstance().getExternalContext().getSessionMap().containsKey("currentTenantId")) {
                selectedTenant = (String) FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("currentTenantId");
                selectedTenantName = TenantConfigCache.getTenantConfig(selectedTenant).getNome();
                currentTenantSessionCreated = true;

            }
        }
    }

    public boolean isShowTenantSelection(){
        return StringUtils.isBlank(selectedTenant);
    }

    public String login() {

        if(!currentTenantSessionCreated){
            TenantContext.setTenantId(selectedTenant);
            // Store in session attribute
            FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("currentTenantId", selectedTenant);
            selectedTenantName = TenantConfigCache.getTenantConfig(selectedTenant).getNome();
        }

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
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}