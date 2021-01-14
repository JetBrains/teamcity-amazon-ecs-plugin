<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/include.jsp" %>

<%--
  ~ Copyright 2000-2021 JetBrains s.r.o.
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~ http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  --%>

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