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
<link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/css/bootstrap.min.css" integrity="sha384-ggOyR0iXCbMQv3Xipma34MD+dH/1fQ784/j6cY/iJTQUOhcWr7x9JvoRxT2MZw1T" crossorigin="anonymous">
<link rel="stylesheet" href="https://cdn.datatables.net/1.10.20/css/dataTables.bootstrap4.min.css" crossorigin="anonymous">

<!-- Optional JavaScript -->
<!-- jQuery first, then Popper.js, then Bootstrap JS -->
<script src="https://code.jquery.com/jquery-3.4.1.js" integrity="sha256-WpOohJOqMqqyKL9FccASB9O0KwACQJpFTUBLTYOVvVU=" crossorigin="anonymous"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/popper.js/1.14.7/umd/popper.min.js" integrity="sha384-UO2eT0CpHqdSJQ6hJty5KVphtPhzWj9WO1clHTMGa3JDZwrnQq4sF86dIHNDz0W1" crossorigin="anonymous"></script>
<script src="https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/js/bootstrap.min.js" integrity="sha384-JjSmVgyd0p3pXB1rRibZUAYoIIy6OrQ6VrjIEaFf/nJGzIxFDsf4x0xIM+B07jRM" crossorigin="anonymous"></script>
<script src="https://cdn.datatables.net/1.10.20/js/jquery.dataTables.min.js" crossorigin="anonymous"></script>
<script src="https://cdn.datatables.net/1.10.20/js/dataTables.bootstrap4.min.js" crossorigin="anonymous"></script>

<script>
	$(document).ready(function() {
		$('#data').DataTable();
	});
</script>
</head>
<body>
	<%
		List<RequestData> list = RequestDataDao.getRequests();
	%>
	<div class="jumbotron jumbotron-fluid">
		<div class="container">
			<h1 class="display-3">Service Sample</h1>
			<p class="lead">List of Request Since the Server is Running</p>
			<hr class="my-2">
		</div>
	</div>
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
					<th>From</th>
					<th>Type</th>
					<th>Date</th>
					<th>Params</th>
					<th>Payload</th>
				</tr>
			</thead>
			<tbody>
				<%
					if (list.size() > 0)
						for (RequestData data : list) {
				%>
				<tr>
					<td scope="row"><%=data.getIp()%></td>
					<td><%=data.getType()%></td>
					<td><%=DateUtil.parseDateToString(data.getDate())%></td>
					<td><%=data.getParameters()%></td>
					<td><%=data.getPayload()%></td>
				</tr>
				<%
					}
					else {
				%>
				<tr>
					<td colspan="5">No records found!</td>
				</tr>
				<%
					}
				%>
			</tbody>
		</table>
	</div>

</body>
</html>