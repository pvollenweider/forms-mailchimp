<%@ page contentType="text/javascript" %>
<%@ taglib prefix="formfactory" uri="http://www.jahia.org/formfactory/functions" %>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>

(function() {
    var mailchimpMapper = function(ffTemplateResolver) {
        return {
            restrict: 'E',
            templateUrl: function(el, attrs) {
                return ffTemplateResolver.resolveTemplatePath('${formfactory:addFormFactoryModulePath('/form-factory-prefills/mailchimp-mapper', renderContext)}', attrs.viewType);
            },
            scope:{},
            controller: MailchimpMapperController,
            controllerAs: 'mcc',
            link: linkFunc
        };

        function linkFunc (scope, el, attr, ctrl) {}
    };
    angular
        .module('formFactory')
        .directive('ffMailchimpMapper', ['ffTemplateResolver', mailchimpMapper]);

    var MailchimpMapperController = function(contextualData, $http, $httpParamSerializer,
                                             toaster, i18n, $FBFS) {
        var mcc = this;
        mcc.$onInit = function() {
            var req = {
                url: contextualData.urlBase + contextualData.sitePath + '.retrieveListMergeFields.do',
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded'
                }
            };
            $http(req).then(function(response){
                if (response.data.status == 'success') {
                    mcc.mergeFields = response.data.results;
                    for (var i in mcc.mergeFields) {
                        if (mcc.mergeFields[i].tag == $FBFS.activeInput.name) {
                            mcc.selectedMergeField = mcc.mergeFields[i];
                            break;
                        }
                    }
                    mcc.invalidConfiguration = false;
                } else {
                    var message = 'ff.mailchimp.message.toast.' + response.data.errorType;
                    toaster.pop({
                        type   : 'error',
                        title  : i18n.message(message),
                        toastId: 'mccError' + response.data.errorType,
                        timeout: 3000
                    });
                    mcc.invalidConfiguration = true;
                }
            });
        };

        mcc.hasMergeFields = function() {
            return !_.isEmpty(mcc.mergeFields);
        };

        mcc.mapInput = function() {
            $FBFS.activeInput.label = mcc.selectedMergeField.name;
            $FBFS.activeInput.name = mcc.selectedMergeField.tag;
        };

        mcc.canMapInput = function() {
            return !_.isEmpty(mcc.selectedMergeField) && mcc.selectedMergeField.tag != $FBFS.activeInput.name;
        };
    };
    MailchimpMapperController.$inject = ['contextualData', '$http', '$httpParamSerializer',
        'toaster', 'i18nService', '$FBFormStateService'];
})();