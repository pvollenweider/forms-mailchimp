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
public class RemoveConfiguration extends Action {

    private JCRPublicationService publicationService;

    @Override
    public ActionResult doExecute(HttpServletRequest httpServletRequest, RenderContext renderContext, Resource resource, JCRSessionWrapper session, Map<String, List<String>> map, URLResolver urlResolver) throws Exception {
        final ActionResult actionResult = new ActionResult(HttpServletResponse.SC_OK);
        final JSONObject jsonAnswer = new JSONObject();

        final JCRNodeWrapper siteNode = resource.getNode();
        final JCRNodeWrapper formFactoryFolder;
        if (!siteNode.hasNode(Constants.NODE_FORM_FACTORY)) {
            return actionResult;
        } else {
            formFactoryFolder = siteNode.getNode(Constants.NODE_FORM_FACTORY);
        }
        if (!formFactoryFolder.isNodeType(Constants.MIX_MAILCHIMP_CONFIGURATION)) {
            return actionResult;
        } else {
            formFactoryFolder.removeMixin(Constants.MIX_MAILCHIMP_CONFIGURATION);
        }
        session.save();
        final HashSet<String> languages = new HashSet<>();
        languages.add(LanguageCodeConverters.localeToLanguageTag(session.getLocale()));
        publicationService.publishByMainId(formFactoryFolder.getIdentifier(), org.jahia.api.Constants.EDIT_WORKSPACE, org.jahia.api.Constants.LIVE_WORKSPACE, languages, false, null);
        jsonAnswer.put(Constants.ATTR_STATUS, Constants.VALUE_SUCCESS);
        jsonAnswer.put(Constants.ATTR_MESSAGE, "Successfully removed Mailchimp configuration!");
        actionResult.setJson(jsonAnswer);
        return actionResult;
    }

    public void setPublicationService(JCRPublicationService publicationService) {
        this.publicationService = publicationService;
    }
}
