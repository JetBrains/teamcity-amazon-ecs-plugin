<%@ include file="/include.jsp" %>

<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ taglib prefix="util" uri="/WEB-INF/functions/util" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="admin" tagdir="/WEB-INF/tags/admin" %>

<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>
<jsp:useBean id="cons" class="jetbrains.buildServer.clouds.ecs.EcsParameterConstants"/>
<jsp:useBean id="agentPools" scope="request" type="java.util.Collection<jetbrains.buildServer.serverSide.agentPools.AgentPool>"/>

<jsp:useBean id="testConnectionUrl" class="java.lang.String" scope="request"/>
<jsp:useBean id="taskDefChooserUrl" scope="request" type="java.lang.String" />
<jsp:useBean id="clusterChooserUrl" scope="request" type="java.lang.String" />

<script type="text/javascript">
    BS.LoadStyleSheetDynamically("<c:url value='${teamcityPluginResourcesPath}ecsSettings.css'/>");
</script>

</table>

<table class="runnerFormTable">

<jsp:include page="editAWSCommonParams.jsp" />

    <tr>
        <th class="noBorder"></th>
        <td class="noBorder">
            <forms:button id="ecsTestConnectionButton" onclick="BS.Ecs.ProfileSettingsForm.testConnection();">Test connection</forms:button>
        </td>
    </tr>

</table>

<h2 class="noBorder section-header">Agent images</h2>

<div class="buttonsWrapper">
    <div class="imagesTableWrapper hidden">
        <table id="ecsImagesTable" class="settings imagesTable hidden">
            <tbody>
            <tr>
                <th class="name">Task Definition</th>
                <th class="name">Cluster</th>
                <th class="name">Task Group</th>
                <th class="name">Max # of instances</th>
                <th class="name" colspan="2"></th>
            </tr>
            </tbody>
        </table>
        <c:set var="sourceImagesJson" value="${propertiesBean.properties['source_images_json']}"/>
        <input type="hidden" class="jsonParam" name="prop:source_images_json" id="source_images_json" value="<c:out value='${sourceImagesJson}'/>"/>
        <input type="hidden" id="initial_images_list"/>
    </div>
    <forms:addButton title="Add image" id="showAddImageDialogButton">Add image</forms:addButton>
</div>

<bs:dialog dialogId="testConnectionDialog" dialogClass="vcsRootTestConnectionDialog" title="Test Connection" closeCommand="BS.TestConnectionDialog.close();"
           closeAttrs="showdiscardchangesmessage='false'">
    <div id="testConnectionStatus"></div>
    <div id="testConnectionDetails" class="mono"></div>
</bs:dialog>

<bs:dialog dialogId="EcsImageDialog" title="Add Amazon EC2 Container Service Cloud Image" closeCommand="BS.Ecs.ImageDialog.close()"
           dialogClass="EcsImageDialog" titleId="EcsImageDialogTitle">
    <table class="runnerFormTable paramsTable">
        <tr>
            <th>Task Definition:&nbsp;<l:star/></th>
            <td>
                <div>
                    <input type="text" id="${cons.taskDefinition}" value="" class="longField" data-id="${cons.taskDefinition}" data-err-id="${cons.taskDefinition}">
                    <i class="icon-magic" style="cursor:pointer;" title="Choose task definition" onclick="BS.Ecs.TaskDefChooser.showPopup(this, '<c:url value="${taskDefChooserUrl}"/>')"></i>
                    <div class="smallNoteAttention">The family and revision (family:revision) or full Amazon Resource Name (ARN) of the task definition to run. If a revision is not specified, the latest ACTIVE revision is used.</div>
                    <span class="error option-error option-error_${cons.taskDefinition}"></span>
                </div>
            </td>
        </tr>
        <tr class="advancedSetting">
            <th><label for="${cons.agentNamePrefix}">Agent name prefix:</label></th>
            <td><input type="text" id="${cons.agentNamePrefix}" class="longField configParam"/>
                <span id="error_${cons.agentNamePrefix}" class="error option-error option-error_${cons.agentNamePrefix}"></span>
                <span class="smallNote">If no or incorrect prefix provided, default value <strong>ECS</strong> will be used</span>
            </td>
        </tr>
        <tr class="advancedSetting">
            <th>Cluster:</th>
            <td>
                <div>
                    <input type="text" id="${cons.cluster}" value="" class="longField" data-id="${cons.cluster}" data-err-id="${cons.cluster}">
                    <i class="icon-magic" style="cursor:pointer;" title="Choose cluster" onclick="BS.Ecs.ClusterChooser.showPopup(this, '<c:url value="${clusterChooserUrl}"/>')"></i>
                    <div class="smallNoteAttention">The short name or full Amazon Resource Name (ARN) of the cluster on which to run cloud agents. Leave blank to use the default cluster.</div>
                    <span class="error option-error option-error_${cons.cluster}"></span>
                </div>
            </td>
        </tr>
        <tr class="advancedSetting">
            <th>Task Group:</th>
            <td>
                <div>
                    <input type="text" id="${cons.taskGroup}" value="" class="longField" data-id="${cons.taskGroup}" data-err-id="${cons.taskGroup}"/>
                    <div class="smallNoteAttention">The name of the task group to associate with the cloud agent tasks. Leave blank to use the family name of the task definition.</div>
                    <span class="error option-error option-error_${cons.taskGroup}"></span>
                </div>
            </td>
        </tr>
        <tr class="advancedSetting">
            <th>Max number of instances:</th>
            <td>
                <div>
                    <input type="text" id="${cons.maxInstances}" value="" class="longField" data-id="${cons.maxInstances}" data-err-id="${cons.maxInstances}"/>
                </div>
                <span class="error option-error option-error_${cons.maxInstances}"></span>
            </td>
        </tr>
        <tr class="advancedSetting">
            <th><label for="${cons.agentPoolIdField}">Agent pool:</label></th>
            <td>
                <select id="${cons.agentPoolIdField}" data-id="${cons.agentPoolIdField}" class="longField configParam">
                    <props:option value=""><c:out value="<Please select agent pool>"/></props:option>
                    <c:forEach var="ap" items="${agentPools}">
                        <props:option value="${ap.agentPoolId}"><c:out value="${ap.name}"/></props:option>
                    </c:forEach>
                </select>
                <span id="error_${cons.agentPoolIdField}" class="error"></span>
            </td>
        </tr>
    </table>

    <admin:showHideAdvancedOpts containerId="EcsImageDialog" optsKey="ecsCloudSettings"/>
    <admin:highlightChangedFields containerId="EcsImageDialog"/>

    <div class="popupSaveButtonsBlock">
        <forms:submit label="Add" type="button" id="ecsAddImageButton"/>
        <forms:button title="Cancel" id="ecsCancelAddImageButton">Cancel</forms:button>
    </div>
</bs:dialog>

<script type="text/javascript">
    $j.ajax({
        url: "<c:url value="${teamcityPluginResourcesPath}ecsSettings.js"/>",
        dataType: "script",
        cache: true,
        success: function () {
            BS.Ecs.ProfileSettingsForm.testConnectionUrl = '<c:url value="${testConnectionUrl}"/>';
            BS.Ecs.ProfileSettingsForm.initialize();
        }
    });
</script>

<table class="runnerFormTable" style="margin-top: 3em;">