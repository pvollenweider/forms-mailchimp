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
            scope:{},
            controller: SubscribeToMailchimpController,
            controllerAs: 'stmc',
            link: linkFunc
        };

        function linkFunc (scope, el, attr, ctrl) {}
    };
    angular
        .module('formFactory')
        .directive('ffSubscribeToMailchimp', ['ffTemplateResolver', subscribeToMailchimp]);

    var SubscribeToMailchimpController = function() {
        var stmc = this;

        stmc.$onInit = function () {
            console.log('Subscribe To Mailchimp Controller Initialized!');
        };
    };
    SubscribeToMailchimpController.$inject = [];
})();