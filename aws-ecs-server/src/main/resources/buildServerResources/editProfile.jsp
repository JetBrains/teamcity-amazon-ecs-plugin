<%@ include file="/include.jsp" %>

<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ taglib prefix="util" uri="/WEB-INF/functions/util" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="admin" tagdir="/WEB-INF/tags/admin" %>

<%--
  ~ Copyright 2000-2020 JetBrains s.r.o.
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

<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>
<jsp:useBean id="cons" class="jetbrains.buildServer.clouds.ecs.EcsParameterConstants"/>
<jsp:useBean id="launchTypes" scope="request" type="java.util.Collection<com.amazonaws.services.ecs.model.LaunchType>"/>
<jsp:useBean id="agentPools" scope="request" type="java.util.Collection<jetbrains.buildServer.serverSide.agentPools.AgentPool>"/>

<jsp:useBean id="testConnectionUrl" class="java.lang.String" scope="request"/>
<jsp:useBean id="taskDefChooserUrl" scope="request" type="java.lang.String" />
<jsp:useBean id="clusterChooserUrl" scope="request" type="java.lang.String" />
<jsp:useBean id="deleteImageUrl" class="java.lang.String" scope="request"/>

<script type="text/javascript">
    BS.LoadStyleSheetDynamically("<c:url value='${teamcityPluginResourcesPath}ecsSettings.css'/>");
</script>

</table>

<table class="runnerFormTable">

<jsp:include page="editAWSCommonParams.jsp">
    <jsp:param name="requireRegion" value="${true}"/>
    <jsp:param name="requireEnvironment" value="${false}"/>
</jsp:include>

    <tr>
        <th class="noBorder"></th>
        <td class="noBorder">
            <forms:button id="ecsTestConnectionButton" onclick="BS.Ecs.ProfileSettingsForm.testConnection();">Test connection</forms:button>
        </td>
    </tr>

    <tr class="advancedSetting">
        <th><label for="${cons.profileInstanceLimit}">Maximum instances count:</label></th>
        <td>
            <props:textProperty name="${cons.profileInstanceLimit}" className="settings longField"/>
            <span id="error_${cons.profileInstanceLimit}" class="error"></span>
            <span class="smallNote">Maximum number of instances that can be started. Use blank to have no limit</span>
        </td>
    </tr>

</table>

<h2 class="noBorder section-header">Agent images</h2>

<div class="buttonsWrapper">
    <div class="imagesTableWrapper hidden">
        <table id="ecsImagesTable" class="settings imagesTable hidden">
            <tbody>
            <tr>
                <th class="name">Launch type</th>
                <th class="name">Task definition</th>
                <th class="name">Cluster</th>
                <th class="name">Task group</th>
                <th class="name">Max # of instances</th>
                <th class="name">Max cluster CPU reservation (%)</th>
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

<bs:dialog dialogId="EcsImageDialog" title="Add Amazon Elastic Container Service Cloud Image" closeCommand="BS.Ecs.ImageDialog.close()"
           dialogClass="EcsImageDialog" titleId="EcsImageDialogTitle">
    <table class="runnerFormTable paramsTable">
        <tr>
            <th>Launch type:&nbsp;<l:star/></th>
            <td>
                <div style="white-space: nowrap">
                    <select id="${cons.launchType}" data-id="${cons.launchType}" class="longField configParam">
                        <props:option value=""><c:out value="<Please select launch type>"/></props:option>
                        <c:forEach var="launchType" items="${launchTypes}">
                            <props:option selected="${launchType eq propertiesBean.properties['launchType']}" value="${launchType}"><c:out value="${launchType}"/></props:option>
                        </c:forEach>
                    </select>
                </div>
                <div class="smallNoteAttention">The launch type on which to run tasks.</div>
                <span class="error option-error option-error_${cons.launchType}"></span>
            </td>
        </tr>
        <tr>
            <th>Task definition:&nbsp;<l:star/></th>
            <td>
                <div style="white-space: nowrap">
                    <input type="text" id="${cons.taskDefinition}" value="" class="longField" data-id="${cons.taskDefinition}" data-err-id="${cons.taskDefinition}">
                    <i class="icon-magic" style="cursor:pointer;" title="Choose task definition" onclick="BS.Ecs.TaskDefChooser.showPopup(this, '<c:url value="${taskDefChooserUrl}"/>')"></i>
                </div>
                <div class="smallNoteAttention">The family and revision (family:revision) or full Amazon Resource Name (ARN) of the task definition to run. If a revision is not specified, the latest ACTIVE revision is used.</div>
                <span class="error option-error option-error_${cons.taskDefinition}"></span>
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
                <div style="white-space: nowrap">
                    <input type="text" id="${cons.cluster}" value="" class="longField" data-id="${cons.cluster}" data-err-id="${cons.cluster}">
                    <i class="icon-magic" style="cursor:pointer;" title="Choose cluster" onclick="BS.Ecs.ClusterChooser.showPopup(this, '<c:url value="${clusterChooserUrl}"/>')"></i>
                </div>
                <div class="smallNoteAttention">The short name or full Amazon Resource Name (ARN) of the cluster on which to run cloud agents. Leave blank to use the default cluster.</div>
                <span class="error option-error option-error_${cons.cluster}"></span>
            </td>
        </tr>
        <tr class="advancedSetting">
            <th>Task group:</th>
            <td>
                <div>
                    <input type="text" id="${cons.taskGroup}" value="" class="longField" data-id="${cons.taskGroup}" data-err-id="${cons.taskGroup}"/>
                    <div class="smallNoteAttention">The name of the task group to associate with the cloud agent tasks. Leave blank to use the family name of the task definition.</div>
                    <span class="error option-error option-error_${cons.taskGroup}"></span>
                </div>
            </td>
        </tr>
        <tr class="advancedSetting">
            <th>Subnets:</th>
            <td>
                <textarea id="${cons.subnets}" wrap="off" class="subnetList" data-id="${cons.subnets}" data-err-id="${cons.subnets}"></textarea>
                <div class="smallNoteAttention">New line delimited list of subnet ARNs in cluster VPC that TeamCity should consider for task placement.</div>
                <span class="error option-error option-error_${cons.subnets}"></span>
            </td>
        </tr>
        <tr class="advancedSetting">
            <th>Security Groups:</th>
            <td>
                <textarea id="${cons.securityGroups}" wrap="off" class="securityGroupList" data-id="${cons.securityGroups}" data-err-id="${cons.securityGroups}"></textarea>
                <div class="smallNoteAttention">New line delimited list of security group IDs in cluster VPC that TeamCity should apply to the task if run with the networking mode awsvpc. If left blank, the default VPC security group will be used.</div>
                <span class="error option-error option-error_${cons.securityGroups}"></span>
            </td>
        </tr>
        <tr class="advancedSetting">
            <th>Assign public IP:</th>
            <td>
                <input type="checkbox" id="${cons.assignPublicIp}" wrap="off" data-id="${cons.assignPublicIp}" data-err-id="${cons.assignPublicIp}"/>
                <div class="smallNoteAttention">Enable or disable auto-assign public IP.</div>
                <span class="error option-error option-error_${cons.assignPublicIp}"></span>
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
            <th><label for="${cons.cpuReservationLimit}">Max cluster CPU reservation (%):</label></th>
            <td>
                <input type="text" id="${cons.cpuReservationLimit}" value="" class="longField" data-id="${cons.cpuReservationLimit}" data-err-id="${cons.cpuReservationLimit}"/>
                <span class="smallNote">Maximum allowed cluster CPU reservation percentile. Will deny to start new cloud instances when limit is being reached. Use blank to have no limit.</span>
                <span class="error option-error option-error_${cons.cpuReservationLimit}"></span>
            </td>
        </tr>
        <tr>
            <th><label for="${cons.agentPoolIdField}">Agent pool:&nbsp;<l:star/></label></th>
            <td>
                <select id="${cons.agentPoolIdField}" data-id="${cons.agentPoolIdField}" class="longField configParam">
                    <props:option value=""><c:out value="<Please select agent pool>"/></props:option>
                    <c:forEach var="ap" items="${agentPools}">
                        <props:option selected="${ap.agentPoolId eq propertiesBean.properties['agent_pool_id']}" value="${ap.agentPoolId}"><c:out value="${ap.name}"/></props:option>
                    </c:forEach>
                </select>
                <span class="error option-error option-error_${cons.agentPoolIdField}"></span>
            </td>
        </tr>
    </table>

    <admin:showHideAdvancedOpts containerId="EcsImageDialog" optsKey="ecsImageSettings"/>
    <admin:highlightChangedFields containerId="EcsImageDialog"/>

    <div class="popupSaveButtonsBlock">
        <forms:submit label="Add" type="button" id="ecsAddImageButton"/>
        <forms:button title="Cancel" id="ecsCancelAddImageButton">Cancel</forms:button>
    </div>
</bs:dialog>

<bs:dialog dialogId="EcsDeleteImageDialog" title="Delete Amazon ECS Cloud Image" closeCommand="BS.Ecs.DeleteImageDialog.close()"
           dialogClass="EcsDeleteImageDialog" titleId="EcsDeleteImageDialogTitle">

    <div id="ecsDeleteImageDialogBody"></div>

    <div class="popupSaveButtonsBlock">
        <forms:submit label="Delete" type="button" id="ecsDeleteImageButton"/>
        <forms:button title="Cancel" id="ecsCancelDeleteImageButton">Cancel</forms:button>
    </div>
</bs:dialog>

<script type="text/javascript">
    $j.ajax({
        url: "<c:url value="${teamcityPluginResourcesPath}ecsSettings.js"/>",
        dataType: "script",
        cache: true,
        success: function () {
            BS.Ecs.ProfileSettingsForm.testConnectionUrl = '<c:url value="${testConnectionUrl}"/>';
            BS.Ecs.DeleteImageDialog.url = '<c:url value="${deleteImageUrl}"/>';
            BS.Ecs.ProfileSettingsForm.initialize();
        }
    });
</script>

<table class="runnerFormTable" style="margin-top: 3em;">