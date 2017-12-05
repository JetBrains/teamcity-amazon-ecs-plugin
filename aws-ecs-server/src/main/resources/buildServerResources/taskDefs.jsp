<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/include.jsp" %>

<script type="text/javascript">
    BS.LoadStyleSheetDynamically("<c:url value='${teamcityPluginResourcesPath}ecsSettings.css'/>");
</script>

<jsp:useBean id="taskDefs" scope="request" type="java.util.Collection<jetbrains.buildServer.clouds.ecs.apiConnector.EcsTaskDefinition>"/>
<jsp:useBean id="error" scope="request" type="java.lang.String"/>

<c:choose>
    <c:when test="${not empty error}">
        <span class="testConnectionFailed"><c:out value="${error}"/></span>
    </c:when>
    <c:otherwise>
        <c:choose>
            <c:when test="${empty taskDefs}">
                No task definistions found
            </c:when>
            <c:otherwise>
                <ul class="chooser">
                    <c:forEach var="taskDef" items="${taskDefs}">
                        <li value="${taskDef.arn}">
                            <a style="cursor:pointer;" onclick="BS.Ecs.TaskDefChooser.selectTaskDef('${taskDef.displayName}')"><c:out value="${taskDef.displayName}"/></a>
                            <i><c:out value="${taskDef.requiresCompatibilitiesString}"/></i>
                        </li>
                    </c:forEach>
                </ul>
            </c:otherwise>
        </c:choose>
    </c:otherwise>
</c:choose>