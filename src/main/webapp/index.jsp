<%@page import="util.DateUtil"%>
<%@page import="dao.RequestDataDao"%>
<%@page import="entity.RequestData"%>
<%@page import="java.util.List"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!doctype html>
<html lang="en">
<head>
	<title>Title</title>
	<!-- Required meta tags -->
	<meta charset="utf-8">
	<meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">

	<!-- Bootstrap CSS -->
	<link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/css/bootstrap.min.css" crossorigin="anonymous">
	<link rel="stylesheet" href="https://cdn.datatables.net/1.10.20/css/dataTables.bootstrap4.min.css" crossorigin="anonymous">

	<!-- Optional JavaScript -->
	<script src="https://code.jquery.com/jquery-3.4.1.js" crossorigin="anonymous"></script>
	<script src="https://cdnjs.cloudflare.com/ajax/libs/popper.js/1.14.7/umd/popper.min.js" crossorigin="anonymous"></script>
	<script src="https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/js/bootstrap.min.js" crossorigin="anonymous"></script>
	<script src="https://cdn.datatables.net/1.10.20/js/jquery.dataTables.min.js" crossorigin="anonymous"></script>
	<script src="https://cdn.datatables.net/1.10.20/js/dataTables.bootstrap4.min.js" crossorigin="anonymous"></script>

	<script>
		$(document).ready(function() {
			$('#data').DataTable({
				autoWidth: false
			});
		});
	</script>
</head>
<body>
<%
	RequestDataDao dao = (RequestDataDao) request.getAttribute("requestDataDao");
	if (dao == null) {
		response.sendRedirect(request.getContextPath() + "/");
		return;
	}
	List<RequestData> list = dao.getRequests();
%>
<div class="jumbotron jumbotron-fluid">
	<div class="container">
		<h1 class="display-3">Service Sample</h1>
		<p class="lead">List of Request Since the Server is Running</p>
		<hr class="my-2">
	</div>
</div>
<div class="container-fluid">
	<div class="row">
		<div class="col-md-9">
			<div class="container border rounded w-100">
				<a href="clear">
					<button class="btn">
						<span class="badge badge-info">Clear Data</span>
					</button>
				</a>
			</div>
			<div class="container border rounded w-100">
				<br />
				<table id="data" class="table table-striped table-inverse table-responsive table-hover w-100">
					<thead class="thead-dark">
					<tr>
						<th>Id</th>
						<th>From</th>
						<th>Type</th>
						<th>Date</th>
						<th>Params</th>
						<th>Payload</th>
						<th>Header</th>
					</tr>
					</thead>
					<tbody>
					<%
						if (list.size() > 0)
							for (RequestData data : list) {
					%>
					<tr>
						<td>
							<a href="request-data?id=<%=data.getId()%>"><%=data.getId()%></a>
						</td>
						<td scope="row"><%=data.getIp()%></td>
						<td><%=data.getType()%></td>
						<td><%=DateUtil.parseDateToString(data.getDate())%></td>
						<td><%=data.getParameters()%></td>
						<td><%=data.getPayload()%></td>
						<td><%=data.getHeaderListAsString()%></td>
					</tr>
					<%
						}
					else {
					%>
					<tr>
						<td colspan="6">No records found!</td>
					</tr>
					<%
						}
					%>
					</tbody>
				</table>
			</div>
		</div>
		<div class="col-md-3">
			<div class="card mt-3">
				<div class="card-header bg-info text-white">
					How to Test the Service
				</div>
				<div class="card-body">
					<p>
						You can test this app by sending HTTP <b>GET</b> or <b>POST</b> requests to the <code>/service</code> endpoint.
					</p>
					<ul>
						<li>
							<b>GET example:</b><br>
							<code>curl "http://localhost:8080/&lt;your-app-context&gt;/service?foo=bar"</code>
						</li>
						<li class="mt-2">
							<b>POST example:</b><br>
							<code>curl -X POST -d "foo=bar" "http://localhost:8080/&lt;your-app-context&gt;/service"</code>
						</li>
					</ul>
					<p>
						After making a request, <b>refresh this page</b> to see the new entry in the table.
					</p>
				</div>
			</div>
		</div>
	</div>
</div>
</body>
</html>