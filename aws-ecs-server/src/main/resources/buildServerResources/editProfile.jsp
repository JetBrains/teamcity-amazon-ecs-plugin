<%@ include file="/include.jsp" %>

<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ taglib prefix="util" uri="/WEB-INF/functions/util" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="admin" tagdir="/WEB-INF/tags/admin" %>

<script type="text/javascript">
    BS.LoadStyleSheetDynamically("<c:url value='${teamcityPluginResourcesPath}ecsSettings.css'/>");
</script>

</table>

<table class="runnerFormTable">

<jsp:include page="editAWSCommonParams.jsp" />

</table>

<h2 class="noBorder section-header">Agent images</h2>

<script type="text/javascript">
    $j.ajax({
        url: "<c:url value="${teamcityPluginResourcesPath}ecsSettings.js"/>",
        dataType: "script",
        cache: true,
        success: function () {
            BS.ECS.ProfileSettingsForm.initialize();
        }
    });
</script>

<table class="runnerFormTable" style="margin-top: 3em;">