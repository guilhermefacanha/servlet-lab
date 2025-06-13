
package servlet;

import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import dao.RequestDataDao;

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
