package servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import dao.RequestDataDao;
import entity.RequestData;

@SuppressWarnings("unused")
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
		String client = getClientIpAddr(req);
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

		RequestDataDao.add(RequestData.builder().date(new Date()).type(type).ip(client).parameters(params.toString())
				.payload(payload.toString()).build());

	}

	private Map<String, String> getRequestHeadersInMap(HttpServletRequest request) {

		Map<String, String> result = new HashMap<>();

		Enumeration headerNames = request.getHeaderNames();
		while (headerNames.hasMoreElements()) {
			String key = (String) headerNames.nextElement();
			String value = request.getHeader(key);
			result.put(key, value);
			System.out.println(key + ":" + value);
		}

		return result;
	}

	private static String getClientIp(HttpServletRequest request) {

		String remoteAddr = "";

		if (request != null) {
			remoteAddr = request.getHeader("X-FORWARDED-FOR");
			if (remoteAddr == null || "".equals(remoteAddr)) {
				remoteAddr = request.getRemoteAddr();
			}
		}
		System.out.println(remoteAddr);
		return remoteAddr;
	}

	public static String getClientIpAddr(HttpServletRequest request) {
		String ip = request.getHeader("X-Forwarded-For");
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("Proxy-Client-IP");
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("WL-Proxy-Client-IP");
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("HTTP_CLIENT_IP");
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("HTTP_X_FORWARDED_FOR");
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getRemoteAddr();
		}

		System.out.println(ip);
		return ip;
	}
}