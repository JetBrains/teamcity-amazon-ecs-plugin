/*
 * Copyright 2000-2021 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

if (!BS) BS = {};
if (!BS.Ecs) BS.Ecs = {};

if(!BS.Ecs.ProfileSettingsForm) BS.Ecs.ProfileSettingsForm = OO.extend(BS.PluginPropertiesForm, {

    testConnectionUrl: '',

    _dataKeys: [ 'launchType', 'taskDefinition', 'agentNamePrefix', 'cluster', 'taskGroup', 'subnets', 'fargatePlatformVersion', 'securityGroups', 'assignPublicIp', 'maxInstances', 'cpuReservationLimit', 'agent_pool_id'],

    templates: {
        imagesTableRow: $j('<tr class="imagesTableRow">\
<td class="launchType highlight"></td>\
<td class="taskDefinition highlight"></td>\
<td class="cluster highlight"></td>\
<td class="taskGroup highlight"></td>\
<td class="maxInstances highlight"></td>\
<td class="cpuReservationLimit highlight"></td>\
<td class="edit highlight"><a href="#" class="editVmImageLink">edit</a></td>\
<td class="remove"><a href="#" class="removeVmImageLink">delete</a></td>\
        </tr>')},

    selectors: {
        rmImageLink: '.removeVmImageLink',
        editImageLink: '.editVmImageLink',
        imagesTableRow: '.imagesTableRow'
    },

    defaults: {
        launchType: '!SHOULD_NOT_BE_EMPTY!',
        taskDefinition: '!SHOULD_NOT_BE_EMPTY!',
        cluster: '<Default>',
        taskGroup: 'family:<Task Definition Name>',
        maxInstances: '<Unlimited>',
        cpuReservationLimit: '<Unlimited>'
    },

    _errors: {
        badParam: 'Bad parameter',
        required: 'This field cannot be blank',
        requiredForFargate: 'This field is required when using FARGATE launch type',
        notSelected: 'Something should be selected',
        nonNegative: 'Must be non-negative number',
        nonPercentile: 'Must be a number from range 1..100'
    },

    _displayedErrors: {},

    initialize: function(){
        this.$imagesTable = $j('#ecsImagesTable');
        this.$imagesTableWrapper = $j('.imagesTableWrapper');
        this.$emptyImagesListMessage = $j('.emptyImagesListMessage'); //TODO: implement
        this.$showAddImageDialogButton = $j('#showAddImageDialogButton');

        //add / edit image dialog
        this.$addImageButton = $j('#ecsAddImageButton');
        this.$cancelAddImageButton = $j('#ecsCancelAddImageButton');

        this.$deleteImageButton = $j('#ecsDeleteImageButton');
        this.$cancelDeleteImageButton = $j('#ecsCancelDeleteImageButton');

        this.$launchType = $j('#launchType');
        this.$taskDefinition = $j('#taskDefinition');
        this.$agentNamePrefix = $j('#agentNamePrefix');
        this.$taskGroup = $j('#taskGroup');
        this.$subnets = $j('#subnets');
        this.$fargatePlatformVersion = $j('#fargatePlatformVersion');
        this.$securityGroups = $j('#securityGroups');
        this.$assignPublicIp = $j('#assignPublicIp');
        this.$cluster = $j('#cluster');
        this.$maxInstances = $j('#maxInstances');
        this.$cpuReservationLimit = $j('#cpuReservationLimit');
        this.$agentPoolId = $j('#agent_pool_id');

        this.$imagesDataElem = $j('#' + 'source_images_json');

        var self = this;
        var rawImagesData = this.$imagesDataElem.val() || '[]';
        this._imagesDataLength = 0;
        try {
            var imagesData = JSON.parse(rawImagesData);
            this.imagesData = imagesData.reduce(function (accumulator, imageDataStr) {
                accumulator[self._imagesDataLength++] = imageDataStr;
                return accumulator;
            }, {});
        } catch (e) {
            this.imagesData = {};
            BS.Log.error('bad images data: ' + rawImagesData);
        }

        this._bindHandlers();
        this._renderImagesTable();

        BS.Clouds.Admin.CreateProfileForm.checkIfModified();
    },

    _bindHandlers: function () {
        var self = this;

        this.$showAddImageDialogButton.on('click', this._showDialogClickHandler.bind(this));
        this.$addImageButton.on('click', this._submitDialogClickHandler.bind(this));
        this.$cancelAddImageButton.on('click', this._cancelDialogClickHandler.bind(this));

        this.$imagesTable.on('click', this.selectors.rmImageLink, function () {
            self.showDeleteImageDialog($j(this));
            return false;
        });
        this.$deleteImageButton.on('click', this._submitDeleteImageDialogClickHandler.bind(this));
        this.$cancelDeleteImageButton.on('click', this._cancelDeleteImageDialogClickHandler.bind(this));

        var editDelegates = this.selectors.imagesTableRow + ' .highlight, ' + this.selectors.editImageLink;
        var that = this;
        this.$imagesTable.on('click', editDelegates, function () {
            if (!that.$addImageButton.prop('disabled')) {
                self.showEditImageDialog($j(this));
            }
            return false;
        });

        this.$launchType.on('change', function (e, value) {
            if(value !== undefined) this.$launchType.val(value);
            this._image['launchType'] = this.$launchType.val();
            this.validateOptions(e.target.getAttribute('data-id'));
            var subnetsTr = $j('.fargate-only');
            if(this.$launchType.val() === 'FARGATE'){
                subnetsTr.each(function(){
                    $j(this).removeClass("advancedSetting");
                    $j(this).removeClass("advancedSettingHighlight");
                    $j(this).removeClass("advanced_hidden");
                })
            } else {
                subnetsTr.each(function(){
                    $j(this).addClass("advancedSetting");
                    if($j("tr[class*='advancedSettingHighlight']")) {
                        $j(this).addClass("advancedSettingHighlight");
                    }
                    if($j("tr[class*='advanced_hidden']")) {
                        $j(this).addClass("advanced_hidden");
                    }
                })
            }
        }.bind(this));

        this.$taskDefinition.on('change', function (e, value) {
            if(value !== undefined) this.$taskDefinition.val(value);
            this._image['taskDefinition'] = this.$taskDefinition.val();
            this.validateOptions(e.target.getAttribute('data-id'));
        }.bind(this));

        this.$agentNamePrefix.on('change', function (e, value) {
            if(value !== undefined) this.$agentNamePrefix.val(value);
            this._image['agentNamePrefix'] = this.$agentNamePrefix.val();
            this.validateOptions(e.target.getAttribute('data-id'));
        }.bind(this));

        this.$cluster.on('change', function (e, value) {
            if(value !== undefined) this.$cluster.val(value);
            this._image['cluster'] = this.$cluster.val();
            this.validateOptions(e.target.getAttribute('data-id'));
        }.bind(this));

        this.$taskGroup.on('change', function (e, value) {
            if(value !== undefined) this.$taskGroup.val(value);
            this._image['taskGroup'] = this.$taskGroup.val();
            this.validateOptions(e.target.getAttribute('data-id'));
        }.bind(this));

        this.$subnets.on('change', function (e, value) {
            if(value !== undefined) this.$subnets.val(value);
            this._image['subnets'] = this.$subnets.val();
            this.validateOptions(e.target.getAttribute('data-id'));
        }.bind(this));

        this.$fargatePlatformVersion.on('change', function (e, value) {
            if(value !== undefined) this.$fargatePlatformVersion.val(value);
            this._image['fargatePlatformVersion'] = this.$fargatePlatformVersion.val();
            this.validateOptions(e.target.getAttribute('data-id'));
        }.bind(this));

        this.$securityGroups.on('change', function (e, value) {
            if(value !== undefined) this.$securityGroups.val(value);
            this._image['securityGroups'] = this.$securityGroups.val();
            this.validateOptions(e.target.getAttribute('data-id'));
        }.bind(this));

        this.$assignPublicIp.click(function() {
            this._image['assignPublicIp'] = this.$assignPublicIp.prop('checked');
        }.bind(this));

        this.$maxInstances.on('change', function (e, value) {
            if(value !== undefined) this.$maxInstances.val(value);
            this._image['maxInstances'] = this.$maxInstances.val();
            this.validateOptions(e.target.getAttribute('data-id'));
        }.bind(this));

        this.$cpuReservationLimit.on('change', function (e, value) {
            if(value !== undefined) this.$cpuReservationLimit.val(value);
            this._image['cpuReservationLimit'] = this.$cpuReservationLimit.val();
            this.validateOptions(e.target.getAttribute('data-id'));
        }.bind(this));

        this.$agentPoolId.on('change', function (e, value) {
            if(value !== undefined) this.$agentPoolId.val(value);
            this._image['agent_pool_id'] = this.$agentPoolId.val();
            this.validateOptions(e.target.getAttribute('data-id'));
        }.bind(this));
    },

    _renderImagesTable: function () {
        this._clearImagesTable();

        if (this._imagesDataLength) {
            Object.keys(this.imagesData).forEach(function (imageId) {
                var image = this.imagesData[imageId];
                var src = image['source-id'];
                $j('#initial_images_list').val($j('#initial_images_list').val() + src + ",");
                this._renderImageRow(image, imageId);
            }.bind(this));
        }

        this._toggleImagesTable();
        BS.Clouds.Admin.CreateProfileForm.checkIfModified();
    },

    _clearImagesTable: function () {
        this.$imagesTable.find('.imagesTableRow').remove();
    },

    _toggleImagesTable: function () {
        var toggle = !!this._imagesDataLength;
        this.$imagesTableWrapper.removeClass('hidden');
        this.$emptyImagesListMessage.toggleClass('hidden', toggle);
        this.$imagesTable.toggleClass('hidden', !toggle);
    },

    _renderImageRow: function (props, id) {
        var $row = this.templates.imagesTableRow.clone().attr('data-image-id', id);
        var defaults = this.defaults;

        this._dataKeys.forEach(function (className) {
            $row.find('.' + className).text(props[className] || defaults[className]);
        });

        $row.find(this.selectors.rmImageLink).data('image-id', id);
        $row.find(this.selectors.editImageLink).data('image-id', id);
        this.$imagesTable.append($row);
    },

    _showDialogClickHandler: function () {
        if (! this.$showAddImageDialogButton.attr('disabled')) {
            this.showAddImageDialog();
        }
        return false;
    },

    _submitDialogClickHandler: function() {
        if (this.validateOptions()) {
            if (this.$addImageButton.val().toLowerCase() === 'save') {
                this.editImage(this.$addImageButton.data('image-id'));
            } else {
                this.addImage();
            }
            BS.Ecs.ImageDialog.close();
        }
        return false;
    },

    _cancelDialogClickHandler: function () {
        BS.Ecs.ImageDialog.close();
        return false;
    },

    selectTaskDef: function(taskDef){
        this.$taskDefinition.trigger('change', taskDef || '');
    },

    selectCluster: function(cluster){
        this.$cluster.trigger('change', cluster || '');
    },

    showAddImageDialog: function () {
        $j('#EcsImageDialogTitle').text('Add Amazon Elastic Container Service Cloud Image');

        BS.Hider.addHideFunction('EcsImageDialog', this._resetDataAndDialog.bind(this));
        this.$addImageButton.val('Add').data('image-id', 'undefined');

        this._image = {};

        BS.Ecs.ImageDialog.showCentered();
    },

    showEditImageDialog: function ($elem) {
        var imageId = $elem.parents(this.selectors.imagesTableRow).data('image-id');

        $j('#EcsImageDialogTitle').text('Edit Amazon Elastic Container Service Cloud Image');

        BS.Hider.addHideFunction('EcsImageDialog', this._resetDataAndDialog.bind(this));

        typeof imageId !== 'undefined' && (this._image = $j.extend({}, this.imagesData[imageId]));
        this.$addImageButton.val('Save').data('image-id', imageId);
        if (imageId === 'undefined'){
            this.$addImageButton.removeData('image-id');
        }

        var image = this._image;

        this.$launchType.trigger('change', image['launchType'] || '');
        this.selectTaskDef(image['taskDefinition'] || '');
        this.$agentNamePrefix.trigger('change', image['agentNamePrefix'] || '');
        this.$taskGroup.trigger('change', image['taskGroup'] || '');
        this.$subnets.trigger('change', image['subnets'] || '');
        this.$fargatePlatformVersion.trigger('change', image['fargatePlatformVersion'] || '');
        this.$securityGroups.trigger('change', image['securityGroups'] || '');
        this.$assignPublicIp.prop('checked', image['assignPublicIp'] === 'true' ? image['assignPublicIp'] : '');
        this.selectCluster(image['cluster'] || '');
        this.$maxInstances.trigger('change', image['maxInstances'] || '');
        this.$cpuReservationLimit.trigger('change', image['cpuReservationLimit'] || '');
        this.$agentPoolId.trigger('change', image['agent_pool_id'] || '');

        BS.Ecs.ImageDialog.showCentered();
    },

    _resetDataAndDialog: function () {
        this._image = {};

        this.$launchType.trigger('change', '');
        this.selectTaskDef('');
        this.$agentNamePrefix.trigger('change', '');
        this.$taskGroup.trigger('change', '');
        this.$subnets.trigger('change', '');
        this.$fargatePlatformVersion.trigger('change', 'LATEST');
        this.$securityGroups.trigger('change', '');
        this.$assignPublicIp.prop('checked', '');
        this.selectCluster('');
        this.$maxInstances.trigger('change', '');
        this.$cpuReservationLimit.trigger('change', '');
        this.$agentPoolId.trigger('change', '');
    },

    validateOptions: function (options){
        var isValid = true;

        var validators = {
            launchType : function () {
                var launchType = this._image['launchType'];
                if (!launchType || launchType === '' || launchType === undefined) {
                    this.addOptionError('notSelected', 'launchType');
                    isValid = false;
                }
            }.bind(this),

            taskDefinition : function () {
                if (!this._image['taskDefinition']) {
                    this.addOptionError('required', 'taskDefinition');
                    isValid = false;
                }
            }.bind(this),

            maxInstances: function () {
                var maxInstances = this._image['maxInstances'];
                if (maxInstances && (!$j.isNumeric(maxInstances) || maxInstances < 0 )) {
                    this.addOptionError('nonNegative', 'maxInstances');
                    isValid = false;
                }
            }.bind(this),

            cpuReservationLimit: function () {
                var cpuReservationLimit = this._image['cpuReservationLimit'];
                if (cpuReservationLimit && (!$j.isNumeric(cpuReservationLimit) || cpuReservationLimit < 0 || cpuReservationLimit > 100 )) {
                    this.addOptionError('nonPercentile', 'cpuReservationLimit');
                    isValid = false;
                }
            }.bind(this),

            agent_pool_id : function () {
                var agentPoolId = this._image['agent_pool_id'];
                if (!agentPoolId || agentPoolId === '' || agentPoolId === undefined) {
                    this.addOptionError('notSelected', 'agent_pool_id');
                    isValid = false;
                }
            }.bind(this)
        };

        if (options && ! $j.isArray(options)) {
            options = [options];
        }

        this.clearOptionsErrors(options);

        (options || this._dataKeys).forEach(function(option) {
            if(validators[option]) validators[option]();
        });

        return isValid;
    },

    addOptionError: function (errorKey, optionName) {
        var html;

        if (errorKey && optionName) {
            this._displayedErrors[optionName] = this._displayedErrors[optionName] || [];

            if (typeof errorKey !== 'string') {
                html = this._errors[errorKey.key];
                Object.keys(errorKey.props).forEach(function(key) {
                    html = html.replace('%%'+key+'%%', errorKey.props[key]);
                });
                errorKey = errorKey.key;
            } else {
                html = this._errors[errorKey];
            }

            if (this._displayedErrors[optionName].indexOf(errorKey) === -1) {
                this._displayedErrors[optionName].push(errorKey);
                this.addError(html, $j('.option-error_' + optionName));
            }
        }
    },

    addError: function (errorHTML, target) {
        target.append($j('<div>').html(errorHTML));
    },

    clearOptionsErrors: function (options) {
        (options || this._dataKeys).forEach(function (optionName) {
            this.clearErrors(optionName);
        }.bind(this));
    },

    clearErrors: function (errorId) {
        var target = $j('.option-error_' + errorId);
        if (errorId) {
            delete this._displayedErrors[errorId];
        }
        target.empty();
    },

    addImage: function () {
        var newImageId = this.generateNewImageId(),
            newImage = this._image;
        newImage['source-id'] = newImageId;
        this._renderImageRow(newImage, newImageId);
        this.imagesData[newImageId] = newImage;
        this._imagesDataLength += 1;
        this.saveImagesData();
        this._toggleImagesTable();
    },

    generateNewImageId: function () {
        if($j.isEmptyObject(this.imagesData)) return 1;
        else return Math.max.apply(Math, $j.map(this.imagesData, function callback(currentValue) {
            return currentValue['source-id'];
        })) + 1;
    },

    editImage: function (id) {
        this._image['source-id'] = id;
        this.imagesData[id] = this._image;
        this.saveImagesData();
        this.$imagesTable.find(this.selectors.imagesTableRow).remove();
        this._renderImagesTable();
    },

    removeImage: function (imageId) {
        delete this.imagesData[imageId];
        this._imagesDataLength -= 1;
        this.$imagesTable.find('tr[data-image-id=\'' + imageId + '\']').remove();
        this.saveImagesData();
        this._toggleImagesTable();
    },

    saveImagesData: function () {
        var imageData = Object.keys(this.imagesData).reduce(function (accumulator, id) {
            var _val = $j.extend({}, this.imagesData[id]);

            delete _val.$image;
            accumulator.push(_val);

            return accumulator;
        }.bind(this), []);
        this.$imagesDataElem.val(JSON.stringify(imageData));
    },

    testConnection: function() {
        BS.ajaxRequest(this.testConnectionUrl, {
            parameters: BS.Clouds.Admin.CreateProfileForm.serializeParameters(),
            onFailure: function (response) {
                BS.TestConnectionDialog.show(false, response, null);
            }.bind(this),
            onSuccess: function (response) {
                var wereErrors = BS.XMLResponse.processErrors(response.responseXML, {
                    onConnectionError: function(elem) {
                        BS.TestConnectionDialog.show(false, elem.firstChild.nodeValue, null);
                    }
                }, BS.PluginPropertiesForm.propertiesErrorsHandler);
                if(!wereErrors){
                    BS.TestConnectionDialog.show(true, "", null);
                }
            }.bind(this)
        });
    },

    showDeleteImageDialog: function ($elem) {
        var imageId = $elem.parents(this.selectors.imagesTableRow).data('image-id');

        BS.ajaxUpdater($("ecsDeleteImageDialogBody"), BS.Ecs.DeleteImageDialog.url + window.location.search, {
            method: 'get',
            parameters : {
                imageId : imageId
            },
            onComplete: function() {
                BS.Ecs.DeleteImageDialog.show(imageId);
            }
        });
    },

    _cancelDeleteImageDialogClickHandler: function () {
        BS.Ecs.DeleteImageDialog.close();
        return false;
    },

    _submitDeleteImageDialogClickHandler: function() {
        var imageId = BS.Ecs.DeleteImageDialog.currentImageId;
        BS.ajaxRequest(BS.Ecs.DeleteImageDialog.url + window.location.search, {
            method: 'post',
            parameters : {
                imageId : imageId
            },
            onComplete: function() {
                BS.Ecs.ProfileSettingsForm.removeImage(imageId);
                BS.Ecs.DeleteImageDialog.close();
            }
        });
    }
});

if(!BS.Ecs.ImageDialog) BS.Ecs.ImageDialog = OO.extend(BS.AbstractModalDialog, {
    getContainer: function() {
        return $('EcsImageDialog');
    }
});

if(!BS.Ecs.TaskDefChooser){
    BS.Ecs.TaskDefChooser = new BS.Popup('taskDefChooser', {
        hideDelay: 0,
        hideOnMouseOut: false,
        hideOnMouseClickOutside: true,
        loadingText: "Loading task definitions..."
    });

    BS.Ecs.TaskDefChooser.showPopup = function(nearestElement, dataLoadUrl){
        var serializeParameters = BS.Clouds.Admin.CreateProfileForm.serializeParameters();
        serializeParameters += "&launchType=" + BS.Ecs.ProfileSettingsForm.$launchType.val();
        this.showPopupNearElement(nearestElement, {
            parameters: serializeParameters,
            url: dataLoadUrl,
            shift:{x:15,y:15}
        });
    };

    BS.Ecs.TaskDefChooser.selectTaskDef = function (taskDef) {
        BS.Ecs.ProfileSettingsForm.selectTaskDef(taskDef);
        this.hidePopup();
    };
}

if(!BS.Ecs.ClusterChooser) {
    BS.Ecs.ClusterChooser = new BS.Popup('clusterChooser', {
        hideDelay: 0,
        hideOnMouseOut: false,
        hideOnMouseClickOutside: true,
        loadingText: "Loading clusters..."
    });

    BS.Ecs.ClusterChooser.showPopup = function(nearestElement, dataLoadUrl) {
        this.showPopupNearElement(nearestElement, {
            parameters: BS.Clouds.Admin.CreateProfileForm.serializeParameters(),
            url: dataLoadUrl,
            shift:{x:15,y:15}
        });
    };

    BS.Ecs.ClusterChooser.selectCluster = function (cluster) {
        BS.Ecs.ProfileSettingsForm.$cluster.trigger('change', cluster || '');
        this.hidePopup();
    };
}

if(!BS.Ecs.DeleteImageDialog) BS.Ecs.DeleteImageDialog = OO.extend(BS.AbstractModalDialog, {
    url: '',
    currentImageId: '',

    getContainer: function() {
        return $('EcsDeleteImageDialog');
    },

    show: function (imageId) {
        BS.Ecs.DeleteImageDialog.currentImageId = imageId;
        BS.Ecs.DeleteImageDialog.showCentered();
    }
});