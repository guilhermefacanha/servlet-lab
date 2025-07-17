package app.controller;

import app.dao.RequestDataDao;
import app.entity.RequestData;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Named
@ViewScoped
public class RequestDataController implements Serializable {

    @Inject
    private RequestDataDao dao;

    @Getter
    @Setter
    private RequestData ent;

    @Getter @Setter
    private Long selectedId;

    @Getter @Setter
    private List<RequestData> list;

    public void carregarLista() {
        list = dao.getRequests();
    }

    public void loadSelected() throws IOException {
        if(selectedId != null && selectedId > 0)
            ent = dao.getRequestById(selectedId);
        else
            FacesContext.getCurrentInstance().getExternalContext().redirect("index.xhtml?faces-redirect=true");
    }

    public void updateRequestData() {
        if (ent != null) {
            ent.setUpdateDate(new Date());
            ent = dao.save(ent);
            addInfo("Request data updated successfully.");
        } else {
            addInfo("No request data to update.");
        }
    }

    public void deleteRequestData() throws IOException {
        if (ent != null && ent.getId() != null) {
            dao.deleteById(ent.getId());
            addInfo("Request data deleted successfully.");
            FacesContext.getCurrentInstance().getExternalContext().redirect("index.xhtml?faces-redirect=true");
        } else {
            addInfo("No request data to delete.");
        }
    }

    public void deleteAll(){
        dao.clear();
        addInfo("All request data deleted successfully.");
    }

    public void saveRequestData() {
        addInfo("PrimeFaces info message");
    }

    private void addInfo(String msg) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "INFO", msg));

    }

}
