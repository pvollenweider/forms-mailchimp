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

    MailchimpSettingsController.$inject = ['contextualData', '$FBUtilService', '$http', '$httpParamSerializer'];

    function MailchimpSettingsController (contextualData, $FBU, $http, $httpParamSerializer) {
        var msc = this;
        msc.$onInit = function() {
            var path = ['form' +
            'Factory', 'mailchimpConfiguration'];
            $FBU.getNodeFromPath(contextualData.sitePath, path, 'default').then(function(data){
                if (data != null) {
                    msc.apiKey = data.properties.apiKey.value;
                    msc.mailchimpEnabled = msc.apiKey != null;
                    if (data.properties.listName != null) {
                        msc.listName = data.properties.listName.value;
                    }
                }
            });
        };

        msc.onSubmit = function() {
            var data = {
                apiKey: msc.apiKey
            };
            var req = {
                url: contextualData.urlBase + contextualData.sitePath + '.saveMailchimpConfiguration.do',
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded'
                },
                data: $httpParamSerializer(data)
            };
            $http(req).then(function(response){
            });
        };

        msc.updateMailchimpConfiguration = function() {
            if (!msc.mailchimpEnabled) {
                msc.apiKey = null;
                msc.listName = null;
                var req = {
                    url: contextualData.urlBase + contextualData.sitePath + '.removeMailchimpConfiguration.do',
                    method: 'POST'
                };
                $http(req).then(function (response) {
                });
            }
        };
    }
})();