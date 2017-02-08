<%@ page contentType="text/javascript" %>
<%@ taglib prefix="formfactory" uri="http://www.jahia.org/formfactory/functions" %>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>

(function(){
    var mailchimpSettings = function(ffTemplateResolver) {
        return {
            restrict: 'E',
            scope: {},
            templateUrl: function(el, attrs) {
                return ffTemplateResolver.resolveTemplatePath('${formfactory:addFormFactoryModulePath('/form-factory-formsettings/mailchimp-settings', renderContext)}', attrs.viewType);
            },
            controller: MailchimpSettingsController,
            controllerAs: 'msc',
            bindToController: true,
            link: linkFunc
        };

        function linkFunc(scope, el, attr, ctrl) {}
    };
    angular
        .module('formFactory')
        .directive('ffMailchimpSettings', ['ffTemplateResolver', mailchimpSettings]);

    MailchimpSettingsController.$inject = ['contextualData', '$FBUtilService',
        '$http', '$httpParamSerializer', 'toaster', 'i18nService', '$q', '$sce'];

    function MailchimpSettingsController (contextualData, $FBU, $http,
                                          $httpParamSerializer, toaster, i18n, $q, $sce) {
        var msc = this;
        msc.i18nMessageGetter = i18n.message;

        msc.$onInit = function() {
            var path = ['formFactory', 'mailchimpConfiguration'];
            $FBU.getNodeFromPath(contextualData.sitePath, path, 'default').then(function(data){
                if (data != null) {
                    msc.apiKey = data.properties.apiKey.value;
                    msc.mailchimpEnabled = msc.apiKey != null;
                    if (data.properties.listId != null) {
                        msc.listId = data.properties.listId.value;
                    }
                    if (msc.mailchimpEnabled) {
                        retrieveLists().then(function(response){
                            if (response.data.status == 'error') {
                                toaster.pop({
                                    type   : 'error',
                                    title  : i18n.message('ff.mailchimp.message.incorrectApiKey'),
                                    toastId: 'mscInvalidApiKey',
                                    timeout: 3000
                                });
                                msc.apiKeyValid = false;
                            } else {
                                msc.lists = response.data.lists;
                                if (!(msc.listId in msc.lists)) {
                                    msc.listId = null;
                                }
                                msc.apiKeyValid = true;
                            }
                        });
                    }
                }
            });
        };

        msc.onSubmit = function(key) {
            var data = {};
            data[key] = msc[key];
            performRequest('saveConfiguration', data).then(function(){
                if (key == 'apiKey') {
                    retrieveLists().then(function(response){
                        if (response.data.status == 'success') {
                            toaster.pop({
                                type   : 'success',
                                title  : i18n.message('ff.mailchimp.message.apiKeySaved'),
                                toastId: 'mscApiKeySaved',
                                timeout: 3000
                            });
                            msc.apiKeyValid = true;
                            msc.lists = response.data.lists;
                            if (!(msc.listId in msc.lists)) {
                                msc.listId = null;
                            }
                        } else {
                            toaster.pop({
                                type   : 'error',
                                title  : i18n.message('ff.mailchimp.message.incorrectApiKey'),
                                toastId: 'mscInvalidApiKey',
                                timeout: 3000
                            });
                            msc.apiKeyValid = false;
                            msc.listId = '';
                            msc.lists = null;
                        }
                    });
                } else if (key == 'listId') {
                    toaster.pop({
                        type   : 'success',
                        title  : i18n.format('ff.mailchimp.message.listSaved', msc.lists[msc.listId]),
                        toastId: 'mscListSaved',
                        timeout: 3000
                    });
                }
            });
        };

        msc.updateMailchimpConfiguration = function() {
            if (!msc.mailchimpEnabled) {
                msc.apiKey = null;
                msc.listId = null;
                msc.lists = null;
                msc.apiKeyValid = false;
                performRequest('removeConfiguration').then(function(){
                    toaster.pop({
                        type   : 'success',
                        title  : i18n.message('ff.mailchimp.message.removedConfiguration'),
                        toastId: 'mscRemovedConfiguration',
                        timeout: 3000
                    });
                });
            }
        };

        msc.hasLists = function() {
            return !_.isEmpty(msc.lists);
        };

        msc.getEmptyListMessage = function() {
            var link = 'https://' + msc.apiKey.split('-')[1] + '.admin.mailchimp.com/lists/';
            return $sce.trustAsHtml(i18n.format('ff.mailchimp.message.emptyList', '<a href="' + link + '" target="blank">Mailchimp</a>'));
        };

        msc.refreshLists = function() {
            retrieveLists().then(function(response){
                if (response.data.status == 'success') {
                    toaster.pop({
                        type   : 'success',
                        title  : i18n.message('ff.mailchimp.message.refreshedList'),
                        toastId: 'mscRefreshedList',
                        timeout: 3000
                    });
                    msc.lists = response.data.lists;
                }
            })
        };

        function retrieveLists () {
            return $q(function(resolve) {
                var data = {
                    apiKey: msc.apiKey
                };
                performRequest('retrieveLists', data).then(function(response) {
                    resolve(response);
                });
            });
        }

        function performRequest(action, data) {
            var req = {
                url: contextualData.urlBase + contextualData.sitePath + '.' + action + '.do',
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded'
                }
            };
            if (data != null) {
                req.data = $httpParamSerializer(data)
            }
            return $http(req)
        }
    }
})();