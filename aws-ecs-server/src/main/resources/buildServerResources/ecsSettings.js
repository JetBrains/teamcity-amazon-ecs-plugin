if (!BS) BS = {};
if (!BS.Ecs) BS.Ecs = {};

if(!BS.Ecs.ProfileSettingsForm) BS.Ecs.ProfileSettingsForm = OO.extend(BS.PluginPropertiesForm, {

    _dataKeys: [ 'taskDefinition', 'cluster', 'taskGroup', 'maxInstances' ],

    templates: {
        imagesTableRow: $j('<tr class="imagesTableRow">\
<td class="taskDefinition highlight"></td>\
<td class="cluster highlight"></td>\
<td class="taskGroup highlight"></td>\
<td class="maxInstances highlight"></td>\
<td class="edit highlight"><a href="#" class="editVmImageLink">edit</a></td>\
<td class="remove"><a href="#" class="removeVmImageLink">delete</a></td>\
        </tr>')},

    selectors: {
        rmImageLink: '.removeVmImageLink',
        editImageLink: '.editVmImageLink',
        imagesTableRow: '.imagesTableRow'
    },

    _errors: {
        badParam: 'Bad parameter',
        required: 'This field cannot be blank',
        notSeleted: 'Something should be seleted',
        nonNegative: 'Must be non-negative number'
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
        this.$taskDefinition = $j('#taskDefinition');
        this.$taskGroup = $j('#taskGroup');
        this.$cluster = $j('#cluster');
        this.$maxInstances = $j('#maxInstances');

        this.$imagesDataElem = $j('#' + 'source_images_json');

        var self = this;
        var rawImagesData = this.$imagesDataElem.val() || '[]';
        this._lastImageId = this._imagesDataLength = 0;
        try {
            var imagesData = JSON.parse(rawImagesData);
            this.imagesData = imagesData.reduce(function (accumulator, imageDataStr) {
                accumulator[self._lastImageId++] = imageDataStr;
                self._imagesDataLength++;
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
            var $this = $j(this),
                id = $this.data('image-id'),
                imageName = self.imagesData[id].taskDefinition;

            if (confirm('Are you sure you want to remove the image "' + imageName + '"?')) {
                self.removeImage($this);
            }
            return false;
        });

        var editDelegates = this.selectors.imagesTableRow + ' .highlight, ' + this.selectors.editImageLink;
        var that = this;
        this.$imagesTable.on('click', editDelegates, function () {
            if (!that.$addImageButton.prop('disabled')) {
                self.showEditImageDialog($j(this));
            }
            return false;
        });

        this.$taskDefinition.on('change', function (e, value) {
            if(value) this.$taskDefinition.val(value);
            this._image['taskDefinition'] = this.$taskDefinition.val();
            this.validateOptions(e.target.getAttribute('data-id'));
        }.bind(this));

        this.$cluster.on('change', function (e, value) {
            if(value) this.$cluster.val(value);
            this._image['cluster'] = this.$cluster.val();
            this.validateOptions(e.target.getAttribute('data-id'));
        }.bind(this));

        this.$taskGroup.on('change', function (e, value) {
            if(value) this.$taskGroup.val(value);
            this._image['taskGroup'] = this.$taskGroup.val();
            this.validateOptions(e.target.getAttribute('data-id'));
        }.bind(this));

        this.$maxInstances.on('change', function (e, value) {
            if(value) this.$maxInstances.val(value);
            this._image['maxInstances'] = this.$maxInstances.val();
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

        this._dataKeys.forEach(function (className) {
            $row.find('.' + className).text(props[className] || '<Default>');
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
            if (this.$addImageButton.val().toLowerCase() === 'edit') {
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
        $j('#EcsImageDialogTitle').text('Add Amazon EC2 Container Service Cloud Image');

        BS.Hider.addHideFunction('EcsImageDialog', this._resetDataAndDialog.bind(this));
        this.$addImageButton.val('Add').data('image-id', 'undefined');

        this._image = {};

        BS.Ecs.ImageDialog.showCentered();
    },

    showEditImageDialog: function ($elem) {
        var imageId = $elem.parents(this.selectors.imagesTableRow).data('image-id');

        $j('#EcsImageDialogTitle').text('Edit Amazon EC2 Container Service Cloud Image');

        BS.Hider.addHideFunction('EcsImageDialog', this._resetDataAndDialog.bind(this));

        typeof imageId !== 'undefined' && (this._image = $j.extend({}, this.imagesData[imageId]));
        this.$addImageButton.val('Edit').data('image-id', imageId);
        if (imageId === 'undefined'){
            this.$addImageButton.removeData('image-id');
        }

        var image = this._image;

        this.selectTaskDef(image['taskDefinition'] || '');
        this.$taskGroup.trigger('change', image['taskGroup'] || '');
        this.selectCluster(image['cluster'] || '');
        this.$maxInstances.trigger('change', image['maxInstances'] || '');

        BS.Ecs.ImageDialog.showCentered();
    },

    _resetDataAndDialog: function () {
        this._image = {};

        this.selectTaskDef('');
        this.$taskGroup.trigger('change', '');
        this.selectCluster('');
        this.$maxInstances.trigger('change', '');
    },

    validateOptions: function (options){
        var isValid = true;

        var validators = {

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
        var newImageId = this._lastImageId++,
            newImage = this._image;
        newImage['source-id'] = newImageId;
        this._renderImageRow(newImage, newImageId);
        this.imagesData[newImageId] = newImage;
        this._imagesDataLength += 1;
        this.saveImagesData();
        this._toggleImagesTable();
    },

    editImage: function (id) {
        this._image['source-id'] = id;
        this.imagesData[id] = this._image;
        this.saveImagesData();
        this.$imagesTable.find(this.selectors.imagesTableRow).remove();
        this._renderImagesTable();
    },

    removeImage: function ($elem) {
        delete this.imagesData[$elem.data('image-id')];
        this._imagesDataLength -= 1;
        $elem.parents(this.selectors.imagesTableRow).remove();
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
        this.showPopupNearElement(nearestElement, {
            parameters: BS.Clouds.Admin.CreateProfileForm.serializeParameters(),
            url: dataLoadUrl
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
            url: dataLoadUrl
        });
    };

    BS.Ecs.ClusterChooser.selectCluster = function (cluster) {
        BS.Ecs.ProfileSettingsForm.$cluster.trigger('change', cluster || '');
        this.hidePopup();
    };
}