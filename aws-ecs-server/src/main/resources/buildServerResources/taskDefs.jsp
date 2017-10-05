<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/include.jsp" %>

<jsp:useBean id="taskDefs" scope="request" type="java.util.Collection<jetbrains.buildServer.clouds.ecs.apiConnector.EcsTaskDefinition>"/>

<ul>
    <c:forEach var="taskDef" items="${taskDefs}">
        <li value="${taskDef.arn}"><c:out value="${taskDef.displayName}"/></li>
    </c:forEach>
</ul>