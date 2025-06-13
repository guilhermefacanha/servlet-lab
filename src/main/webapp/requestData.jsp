<%@page import="entity.RequestData"%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
  RequestData data = (RequestData) request.getAttribute("requestData");
%>
<!doctype html>
<html lang="en">
<head>
  <title>Request Data Detail</title>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
  <link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/css/bootstrap.min.css" crossorigin="anonymous">
</head>
<body>
<div class="container mt-5">
  <h2>Request Data Detail</h2>
  <hr>
  <%
    if (data == null) {
  %>
  <div class="alert alert-warning">No data found for the given ID.</div>
  <%
  } else {
  %>
  <table class="table table-bordered">
    <tr>
      <th>ID</th>
      <td><%=data.getId()%></td>
    </tr>
    <tr>
      <th>Type</th>
      <td><%=data.getType()%></td>
    </tr>
    <tr>
      <th>IP</th>
      <td><%=data.getIp()%></td>
    </tr>
    <tr>
      <th>Date</th>
      <td><%=data.getDate()%></td>
    </tr>
    <tr>
      <th>Update Date</th>
      <td><%=data.getUpdateDate()%></td>
    </tr>
    <tr>
      <th>Parameters</th>
      <td><%=data.getParameters()%></td>
    </tr>
    <tr>
      <th>Payload</th>
      <td>
        <pre><code><%= org.apache.commons.lang3.StringEscapeUtils.escapeHtml4(data.getPayload() == null ? "" : data.getPayload()) %></code></pre>
      </td>
    </tr>
    <tr>
      <th>Header</th>
      <td><%=data.getHeaderListAsString()%></td>
    </tr>
  </table>
  <a href="index.jsp" class="btn btn-secondary">Back to List</a>
  <button class="btn btn-primary" onclick="updateRecord(<%=data.getId()%>)">Update</button>
  <button class="btn btn-danger" onclick="deleteRecord(<%=data.getId()%>)">Delete</button>
  <%
    }
  %>
</div>

<script>

  function updateRecord(id) {
    fetch('request-data?id=' + id, { method: 'PUT' })
            .then(response => {
              if (response.ok) {
                window.location.reload();
              } else {
                response.text().then(msg => alert('Update failed: ' + msg));
              }
            })
            .catch(() => alert('Update request failed.'));
  }

  function deleteRecord(id) {
    if (!confirm('Are you sure you want to delete this record?')) return;
    fetch('request-data?id=' + id, { method: 'DELETE' })
            .then(response => {
              if (response.ok) {
                window.location.href = 'index.jsp';
              } else {
                response.text().then(msg => alert('Delete failed: ' + msg));
              }
            })
            .catch(() => alert('Delete request failed.'));
  }
</script>

</body>
</html>