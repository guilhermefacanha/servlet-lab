<%@ page session="true" %>
<html><body>
<h1>Hello from Cluster!</h1>
<p>Session ID: <%= session.getId() %></p>
<p>Node: <%= System.getProperty("jboss.node.name") %></p>
<p>Request served by: <%= request.getLocalName() %>:<%= request.getLocalPort() %></p>
<% session.setAttribute("testAttribute", "testValue-" + System.getProperty("jboss.node.name")); %>
<p>Session attribute 'testAttribute': <%= session.getAttribute("testAttribute") %></p>
</body></html>