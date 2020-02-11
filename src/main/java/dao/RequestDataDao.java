package dao;

import java.util.ArrayList;
import java.util.List;

import entity.RequestData;

public class RequestDataDao {
	private static List<RequestData> requests = new ArrayList<RequestData>();

	private RequestDataDao() throws InstantiationException {
		throw new InstantiationException("Singleton Class!");
	}

	public static void add(RequestData data) {
		requests.add(data);
	}

	public static List<RequestData> getRequests() {
		return requests;
	}

}
