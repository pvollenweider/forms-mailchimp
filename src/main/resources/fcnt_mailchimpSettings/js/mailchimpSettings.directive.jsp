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

    MailchimpSettingsController.$inject = ['i18nService'];

    function MailchimpSettingsController (i18n) {
        var msc = this;
        msc.$onInit = function() {
            console.log('Mailchimp controller initialized');
        }
    }
})();