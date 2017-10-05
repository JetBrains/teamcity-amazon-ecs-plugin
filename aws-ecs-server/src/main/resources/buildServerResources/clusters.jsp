<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/include.jsp" %>

<script type="text/javascript">
    BS.LoadStyleSheetDynamically("<c:url value='${teamcityPluginResourcesPath}ecsSettings.css'/>");
</script>

<jsp:useBean id="clusters" scope="request" type="java.util.Collection<jetbrains.buildServer.clouds.ecs.apiConnector.EcsCluster>"/>

<ul class="chooser">
    <c:forEach var="cluster" items="${clusters}">
        <li value="${cluster.arn}"><a style="cursor:pointer;" onclick="BS.Ecs.ClusterChooser.selectCluster('${cluster.name}')"><c:out value="${cluster.name}"/></a></li>
    </c:forEach>
</ul>