<%@ page contentType="text/javascript" %>
<%@ taglib prefix="formfactory" uri="http://www.jahia.org/formfactory/functions" %>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>

(function() {
    var mailchimpMapper = function(ffTemplateResolver) {
        return {
            restrict: 'E',
            templateUrl: function(el, attrs) {
                return ffTemplateResolver.resolveTemplatePath('${formfactory:addFormFactoryModulePath('/form-factory-misc-directives/mailchimp-mapper', renderContext)}', attrs.viewType);
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
                                             toaster, i18n, $FBFS, ffBucketService) {
        var BUCKET_NAME = "mailChimpBucket";
        var TRACK_BY = "tag";
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
                if (ffBucketService.bucket(BUCKET_NAME) === null) {
                    ffBucketService.createBucket(BUCKET_NAME, response.data.results, TRACK_BY);
                    initBucket();
                }
                mcc.mergeFields = angular.copy(ffBucketService.bucket(BUCKET_NAME));
                var tag = $FBFS.activeInput.miscDirectives['mailchimp-mapper'].tag;

                if (!_.isEmpty(tag))
                    var currentValue = ffBucketService.takenValue(BUCKET_NAME, tag);
                    if (currentValue === undefined) {
                        for (var i in mcc.mergeFields) {
                            if (mcc.mergeFields[i].tag == tag) {
                                mcc.selectedMergeField = ffBucketService.take(BUCKET_NAME, mcc.mergeFields[i].tag);
                                break;
                            }
                        }
                    }
                    else {
                        mcc.mergeFields.push(currentValue);
                        mcc.selectedMergeField = currentValue;
                    }
                mcc.invalidConfiguration = false;
            }, function (error) {
                var message = 'ff.mailchimp.message.toast.' + error.data.errorType;
                toaster.pop({
                    type   : 'error',
                    title  : i18n.message(message),
                    toastId: 'mccError' + error.data.errorType,
                    timeout: 3000
                });
                mcc.invalidConfiguration = true;
            });
        };

        mcc.hasMergeFields = function() {
            return !_.isEmpty(mcc.mergeFields);
        };

        mcc.mapInput = function(field) {
            $FBFS.activeInput.miscDirectives['mailchimp-mapper'].tag = mcc.selectedMergeField.tag;
            ffBucketService.take(BUCKET_NAME, mcc.selectedMergeField.tag);
            ffBucketService.put(BUCKET_NAME, field);
        };

        mcc.reset = function() {
            ffBucketService.put(BUCKET_NAME, mcc.selectedMergeField);
            mcc.selectedMergeField = undefined;
            $FBFS.activeInput.miscDirectives['mailchimp-mapper'].tag = "";
        };

        function initBucket() {
            var steps = $FBFS.getSteps();
            for (var i = 0; i < steps.length; i++) {
                var inputs = $FBFS.getInputsFromStep(i);
                for (var j = 0; j < inputs.length; j++) {
                    var input = inputs[j];
                    if (input.miscDirectives !== undefined && input.miscDirectives['mailchimp-mapper']) {
                        var mapper = input.miscDirectives['mailchimp-mapper'];
                        ffBucketService.take(BUCKET_NAME, mapper.tag);
                    }
                }
            }
        }
    };
    MailchimpMapperController.$inject = ['contextualData', '$http', '$httpParamSerializer',
        'toaster', 'i18nService', '$FBFormStateService', 'ffBucketService'];
})();