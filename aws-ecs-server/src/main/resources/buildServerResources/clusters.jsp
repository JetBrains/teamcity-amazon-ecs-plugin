<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/include.jsp" %>

<jsp:useBean id="clusters" scope="request" type="java.util.Collection<jetbrains.buildServer.clouds.ecs.apiConnector.EcsCluster>"/>

<ul>
    <c:forEach var="cluster" items="${clusters}">
        <li value="${cluster.arn}"><c:out value="${cluster.name}"/></li>
    </c:forEach>
</ul>