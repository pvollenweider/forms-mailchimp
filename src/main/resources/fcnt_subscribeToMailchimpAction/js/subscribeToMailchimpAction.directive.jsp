<%@ page contentType="text/javascript" %>
<%@ taglib prefix="formfactory" uri="http://www.jahia.org/formfactory/functions" %>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>

(function(){
    var subscribeToMailchimp = function(ffTemplateResolver) {
        return {
            restrict: 'E',
            templateUrl: function(el, attrs) {
                return ffTemplateResolver.resolveTemplatePath('${formfactory:addFormFactoryModulePath('/form-factory-actions/subscribe-to-mailchimp', renderContext)}', attrs.viewType);
            },
            controller: SubscribeToMailchimpController,
            controllerAs: 'stmc',
            bindToController:true,
            link: linkFunc
        };

        function linkFunc (scope, el, attr, ctrl) {}
    };
    angular
        .module('formFactory')
        .directive('ffSubscribeToMailchimp', ['ffTemplateResolver', subscribeToMailchimp]);

    var SubscribeToMailchimpController = function($FBFS) {
        var stmc = this;

        stmc.$onInit = function () {
            stmc.action = $FBFS.activeAction;
            stmc.steps = $FBFS.getSteps();
        };

        stmc.updateMappedEmailInput = function(name) {
            $FBFS.activeAction.mappedEmailInput = !_.isEmpty($FBFS.activeAction.mappedEmailInput) && name == $FBFS.activeAction.mappedEmailInput ? '' : name;
        }
    };
    SubscribeToMailchimpController.$inject = ['$FBFormStateService'];
})();