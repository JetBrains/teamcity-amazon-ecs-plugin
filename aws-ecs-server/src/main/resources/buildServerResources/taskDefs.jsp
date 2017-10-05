<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/include.jsp" %>

<script type="text/javascript">
    BS.LoadStyleSheetDynamically("<c:url value='${teamcityPluginResourcesPath}ecsSettings.css'/>");
</script>

<jsp:useBean id="taskDefs" scope="request" type="java.util.Collection<jetbrains.buildServer.clouds.ecs.apiConnector.EcsTaskDefinition>"/>

<ul class="chooser">
    <c:forEach var="taskDef" items="${taskDefs}">
        <li value="${taskDef.arn}"><a style="cursor:pointer;" onclick="BS.Ecs.TaskDefChooser.selectTaskDef('${taskDef.displayName}')"><c:out value="${taskDef.displayName}"/></a></li>
    </c:forEach>
</ul>