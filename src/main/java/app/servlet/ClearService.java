
package app.servlet;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import app.dao.RequestDataDao;

@WebServlet("/clear")
public class ClearService extends HttpServlet{

	private static final long serialVersionUID = -4645128584699214422L;

	@Inject
	private RequestDataDao requestDataDao;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		this.doPost(req, resp);
	}
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		requestDataDao.clear();
		resp.sendRedirect("index.jsp");
	}
	
}
