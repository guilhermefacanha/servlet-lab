package servlet;

import dao.RequestDataDao;
import entity.RequestData;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/request-data")
public class RequestDataServlet extends HttpServlet {

    @Inject
    private RequestDataDao requestDataDao;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String idParam = req.getParameter("id");
        RequestData requestData = null;
        if (idParam != null) {
            try {
                Long id = Long.parseLong(idParam);
                requestData = requestDataDao.getRequestById(id);
            } catch (NumberFormatException ignored) {}
        }
        req.setAttribute("requestData", requestData);
        req.getRequestDispatcher("/requestData.jsp").forward(req, resp);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String idParam = req.getParameter("id");
        if (idParam == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing id parameter");
            return;
        }
        try {
            Long id = Long.parseLong(idParam);
            RequestData data = requestDataDao.getRequestById(id);
            if (data == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Record not found");
                return;
            }
            data.setUpdateDate(new java.util.Date());
            requestDataDao.save(data);
            resp.setStatus(HttpServletResponse.SC_OK);
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid id parameter");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String idParam = req.getParameter("id");
        if (idParam == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing id parameter");
            return;
        }
        try {
            Long id = Long.parseLong(idParam);
            requestDataDao.deleteById(id);
            resp.setStatus(HttpServletResponse.SC_OK);
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid id parameter");
        }
    }
}