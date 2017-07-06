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

<c:set var="AWSPortalLink"><a href="https://aws-portal.amazon.com/gp/aws/developer/account/index.html?ie=UTF8&action=access-key" target="_blank">Amazon Web Services Site</a></c:set>

<c:set var="accessIdKey" value="ecs-secret-key"/>
<c:set var="secretKey" value="ecs-access-key-id"/>

<table class="runnerFormTable">
    <tr>
        <th><label for="secure:${accessIdKey}">Access key ID: <l:star/></label></th>
        <td><props:textProperty name="secure:${accessIdKey}" className="longField"/>
            <span id="error_secure:${accessIdKey}" class="error"></span>
            <span class="smallNote">Your Amazon account Access Key ID. View your access keys at ${AWSPortalLink}</span>
        </td>
    </tr>

    <tr>
        <th><label for="secure:${secretKey}">Secret access key: <l:star/></label></th>
        <td><props:passwordProperty name="secure:${secretKey}" className="longField"/>
            <span id="error_secure:${secretKey}" class="error"></span>
            <span class="smallNote">Your Amazon account Secret Access Key. View your access keys at ${AWSPortalLink}</span>
        </td>
    </tr>
</table>

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