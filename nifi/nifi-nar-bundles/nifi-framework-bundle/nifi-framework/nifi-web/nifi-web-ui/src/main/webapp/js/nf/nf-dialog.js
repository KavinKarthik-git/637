/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* global nf */

$(document).ready(function () {
    // setup general button mouse behavior
    nf.Common.addHoverEffect('div.button', 'button-normal', 'button-over');

    // configure the ok dialog
    $('#nf-ok-dialog').modal({
        buttons: [{
                buttonText: 'Ok',
                handler: {
                    click: function () {
                        // close the dialog
                        $('#nf-ok-dialog').modal('hide');
                    }
                }
            }],
        handler: {
            close: function () {
                // clear the content
                $('#nf-ok-dialog-content').empty();
            }
        }
    }).draggable({
        containment: 'parent',
        handle: '.dialog-header'
    });

    // configure the yes/no dialog
    $('#nf-yes-no-dialog').modal({
        handler: {
            close: function () {
                // clear the content
                $('#nf-yes-no-dialog-content').empty();
            }
        }
    }).draggable({
        containment: 'parent',
        handle: '.dialog-header'
    });
});

nf.Dialog = (function () {

    return {
        /**
         * Shows an general dialog with an Okay button populated with the
         * specified dialog content.
         * 
         * @argument {object} options       Dialog options
         */
        showOkDialog: function (options) {
            options = $.extend({
                headerText: '',
                dialogContent: '',
                overlayBackground: true
            }, options);

            // regardless of whether the dialog is already visible, the new content will be appended
            var content = $('<p></p>').append(options.dialogContent);
            $('#nf-ok-dialog-content').append(content);

            // show the dialog
            $('#nf-ok-dialog').modal('setHeaderText', options.headerText).modal('setOverlayBackground', options.overlayBackground).modal('show');
        },
        
        /**
         * Shows an general dialog with Yes and No buttons populated with the
         * specified dialog content.
         * 
         * @argument {object} options       Dialog options
         */
        showYesNoDialog: function (options) {
            options = $.extend({
                headerText: '',
                dialogContent: '',
                overlayBackgrond: true
            }, options);

            // add the content to the prompt
            var content = $('<p></p>').append(options.dialogContent);
            $('#nf-yes-no-dialog-content').append(content);

            // update the button model
            $('#nf-yes-no-dialog').modal('setButtonModel', [{
                    buttonText: 'Yes',
                    handler: {
                        click: function () {
                            // close the dialog
                            $('#nf-yes-no-dialog').modal('hide');
                            if (typeof options.yesHandler === 'function') {
                                options.yesHandler.call(this);
                            }
                        }
                    }
                }, {
                    buttonText: 'No',
                    handler: {
                        click: function () {
                            // close the dialog
                            $('#nf-yes-no-dialog').modal('hide');
                            if (typeof options.noHandler === 'function') {
                                options.noHandler.call(this);
                            }
                        }
                    }
                }]);

            // show the dialog
            $('#nf-yes-no-dialog').modal('setHeaderText', options.headerText).modal('setOverlayBackground', options.overlayBackground).modal('show');
        }
    };
}());