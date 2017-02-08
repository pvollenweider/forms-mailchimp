package org.jahia.modules.ffmailchimp.actions;

import org.jahia.api.Constants;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Created by stefan on 2017-02-06.
 */
public class RemoveConfiguration extends Action {
    private final static Logger logger = LoggerFactory.getLogger(RemoveConfiguration.class);

    private JCRPublicationService publicationService;

    @Override
    public ActionResult doExecute(HttpServletRequest httpServletRequest, RenderContext renderContext, Resource resource, JCRSessionWrapper session, Map<String, List<String>> map, URLResolver urlResolver) throws Exception {
        ActionResult actionResult = new ActionResult(HttpServletResponse.SC_OK);
        JSONObject jsonAnswer = new JSONObject();

        JCRNodeWrapper siteNode = resource.getNode();
        JCRNodeWrapper formFactoryFolder;
        if (!siteNode.hasNode("formFactory")) {
            return actionResult;
        } else {
            formFactoryFolder = siteNode.getNode("formFactory");
        }
        if (!formFactoryFolder.isNodeType("fcmix:mailchimpConfiguration")) {
            return actionResult;
        } else {
            formFactoryFolder.removeMixin("fcmix:mailchimpConfiguration");
        }
        session.save();
        HashSet<String> languages = new HashSet<>();
        languages.add(LanguageCodeConverters.localeToLanguageTag(session.getLocale()));
        publicationService.publishByMainId(formFactoryFolder.getIdentifier(), Constants.EDIT_WORKSPACE, Constants.LIVE_WORKSPACE, languages, false, null);
        jsonAnswer.put("status", "success");
        jsonAnswer.put("message", "Successfully removed Mailchimp configuration!");
        actionResult.setJson(jsonAnswer);
        return actionResult;
    }

    public void setPublicationService(JCRPublicationService publicationService) {
        this.publicationService = publicationService;
    }
}
