package servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Date;
import java.util.Enumeration;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import dao.RequestDataDao;
import entity.RequestData;

@WebServlet({ "/service" })
public class AsyncService extends HttpServlet {
	private static final long serialVersionUID = 1339049045194824834L;

	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		process(req, resp, "GET");
	}

	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		process(req, resp, "POST");
	}

	private void process(HttpServletRequest req, HttpServletResponse resp, String type) {
		StringBuffer params = new StringBuffer();
		StringBuffer payload = new StringBuffer();
		String line = null;
		String ip = req.getRemoteAddr();
		String header = req.getHeader("X-Forwarded-For");
		try {
			if (ip.equalsIgnoreCase("0:0:0:0:0:0:0:1") || ip.equalsIgnoreCase("127.0.0.1")) {
				InetAddress localip = InetAddress.getLocalHost();
				ip = "HostAddress: " + localip.getHostAddress();
				ip = ip + " , HostName: " + localip.getHostName();
			}
		} catch (Exception exception) {
		}
		String client = ip + " , X-Forwarded-For:" + header;
		Enumeration<String> parameterNames = req.getParameterNames();
		while (parameterNames.hasMoreElements()) {
			String paramName = parameterNames.nextElement();
			String[] paramValues = req.getParameterValues(paramName);
			for (int i = 0; i < paramValues.length; i++) {
				String paramValue = paramValues[i];
				params.append(paramName + "=" + paramValue);
				params.append("\n");
			}
		}
		try {
			BufferedReader reader = req.getReader();
			while ((line = reader.readLine()) != null)
				payload.append(line + "\n");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		System.out.println("Request " + type + " from (" + client + "): \nparams:" + params.toString() + "\npayload:"
				+ payload.toString() + "\n\n==End of Request==");

		RequestDataDao.add(RequestData.builder().date(new Date()).type(type).ip(ip).parameters(params.toString())
				.payload(payload.toString()).build());

	}
}