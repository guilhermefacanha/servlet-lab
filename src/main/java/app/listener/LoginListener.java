package app.listener;


import app.controller.LoginController;
import javax.faces.application.Application;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import javax.servlet.http.HttpSession;
import org.apache.commons.lang3.StringUtils;

public class LoginListener implements PhaseListener {

    private static final long serialVersionUID = 1L;

    public static String TELA_INICIAL = "telaInicial";

    private String[] publico = {"login"};
    private String loginPage = "login";

    public void afterPhase(PhaseEvent event) {
        FacesContext contexto = event.getFacesContext();
        HttpSession session = (HttpSession) contexto.getExternalContext().getSession(false);

        String paginaAtual = getPage(contexto);
        LoginController loginController = (LoginController) contexto.getApplication().evaluateExpressionGet(contexto, "#{loginController}", LoginController.class);

        boolean cssPage = (paginaAtual.lastIndexOf("/css/") > -1) || (paginaAtual.lastIndexOf("/js/") > -1);

        if (!isPublica(paginaAtual) && !cssPage) {
            if (StringUtils.isNotBlank(paginaAtual) && !isLogin(paginaAtual)) {
                if(loginController == null || !loginController.isLoggedIn()) {
                    navegarParaPagina("/login?faces-redirect=true");
                    return;
                }
            }
        }
    }

    private String getPage(FacesContext contexto) {
        String paginaAtual = "";

        try {
            paginaAtual = contexto.getViewRoot().getViewId();
            paginaAtual = paginaAtual.substring(1, paginaAtual.length());
            paginaAtual = paginaAtual.replace(".xhtml", "");
        } catch (Exception e) {
        }
        return paginaAtual;
    }

    private boolean isLogin(String paginaAtual) {
        return paginaAtual.contains(loginPage);
    }

    private boolean isPublica(String paginaAtual) {
        for (String s : publico) {
            if (paginaAtual.contains(s))
                return true;
        }
        return false;
    }

    public static void navegarParaPagina(String pagina) {
        Application app = FacesContext.getCurrentInstance().getApplication();
        app.getNavigationHandler().handleNavigation(FacesContext.getCurrentInstance(), null, pagina);
    }

    public void beforePhase(PhaseEvent arg0) {
    }

    public PhaseId getPhaseId() {
        return PhaseId.RESTORE_VIEW;
    }

}
