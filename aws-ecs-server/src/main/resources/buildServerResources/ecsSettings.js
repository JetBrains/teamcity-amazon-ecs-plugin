if (!BS) BS = {};
if (!BS.Ecs) BS.Ecs = {};

if(!BS.Ecs.ProfileSettingsForm) BS.Ecs.ProfileSettingsForm = OO.extend(BS.PluginPropertiesForm, {

    _dataKeys: [ 'taskDefiniton', 'cluster', 'taskGroup', 'maxInstances' ],

    templates: {
        imagesTableRow: $j('<tr class="imagesTableRow">\
<td class="taskDefiniton highlight"></td>\
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

    initialize: function(){
        this.$imagesTable = $j('#ecsImagesTable');
        this.$imagesTableWrapper = $j('.imagesTableWrapper');
        this.$emptyImagesListMessage = $j('.emptyImagesListMessage'); //TODO: implement
        this.$showAddImageDialogButton = $j('#showAddImageDialogButton');

        //add / edit image dialog
        this.$addImageButton = $j('#ecsAddImageButton');
        this.$cancelAddImageButton = $j('#ecsCancelAddImageButton');

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
            $row.find('.' + className).text(props[className]);
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

    showAddImageDialog: function () {
        $j('#EcsImageDialogTitle').text('Add Amazon EC2 Container Service Cloud Image');

        BS.Hider.addHideFunction('EcsImageDialog', this._resetDataAndDialog.bind(this));
        this.$addImageButton.val('Add').data('image-id', 'undefined');

        this._image = {};

        BS.Ecs.ImageDialog.showCentered();
    },

    _resetDataAndDialog: function () {
        this._image = {};

        // this.$podSpecModeSelector.trigger('change', 'notSelected');
        // this.$dockerImage.trigger('change', '');
        // this.$imagePullPolicy.trigger('change', 'IfNotPresent');
        // this.$dockerCommand.trigger('change', '');
        // this.$dockerArgs.trigger('change', '');
        // this.$deploymentName.trigger('change', '');
        // this.$customPodTemplate.trigger('change', '');
        // this.$maxInstances.trigger('change', '');
    }
});

if(!BS.Ecs.ImageDialog) BS.Ecs.ImageDialog = OO.extend(BS.AbstractModalDialog, {
    getContainer: function() {
        return $('EcsImageDialog');
    }
});