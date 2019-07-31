package org.jahia.modules.forms.mailchimp.actions;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRPublicationService;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.jahia.utils.LanguageCodeConverters;
import org.json.JSONObject;

/**
 * Created by stefan on 2017-02-06.
 */
public class SaveConfiguration extends Action {

    private JCRPublicationService publicationService;

    @Override
    public ActionResult doExecute(HttpServletRequest httpServletRequest, RenderContext renderContext, Resource resource, JCRSessionWrapper session, Map<String, List<String>> map, URLResolver urlResolver) throws Exception {
        final ActionResult actionResult = new ActionResult(HttpServletResponse.SC_OK);

        final JCRNodeWrapper siteNode = resource.getNode();
        final JCRNodeWrapper formFactoryFolder;
        if (!siteNode.hasNode(Constants.NODE_FORM_FACTORY)) {
            formFactoryFolder = siteNode.addNode(Constants.NODE_FORM_FACTORY, Constants.NT_FORM_FACTORY);
            session.save();
        } else {
            formFactoryFolder = siteNode.getNode(Constants.NODE_FORM_FACTORY);
        }
        if (!formFactoryFolder.isNodeType(Constants.MIX_MAILCHIMP_CONFIGURATION)) {
            formFactoryFolder.addMixin(Constants.MIX_MAILCHIMP_CONFIGURATION);
        }
        final JCRNodeWrapper mailchimpConfigurationNode;
        if (!formFactoryFolder.hasNode(Constants.NODE_MAILCHIMP_CONFIGURATION)) {
            mailchimpConfigurationNode = formFactoryFolder.addNode(Constants.NODE_MAILCHIMP_CONFIGURATION, Constants.NT_MAILCHIMP_CONFIGURATION);
        } else {
            mailchimpConfigurationNode = formFactoryFolder.getNode(Constants.NODE_MAILCHIMP_CONFIGURATION);
        }
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            String key = entry.getKey();
            mailchimpConfigurationNode.setProperty(key, entry.getValue().get(0));
        }
        session.save();
        final HashSet<String> languages = new HashSet<>();
        languages.add(LanguageCodeConverters.localeToLanguageTag(session.getLocale()));
        publicationService.publishByMainId(formFactoryFolder.getIdentifier(), org.jahia.api.Constants.EDIT_WORKSPACE, org.jahia.api.Constants.LIVE_WORKSPACE, languages, false, null);
        final JSONObject jsonAnswer = new JSONObject();
        jsonAnswer.put(Constants.ATTR_STATUS, Constants.VALUE_SUCCESS);
        jsonAnswer.put(Constants.ATTR_MESSAGE, "Mailchimp configuration was saved successfully!");
        actionResult.setJson(jsonAnswer);
        return actionResult;
    }

    public void setPublicationService(JCRPublicationService publicationService) {
        this.publicationService = publicationService;
    }
}
