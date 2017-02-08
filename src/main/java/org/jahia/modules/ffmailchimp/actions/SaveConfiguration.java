package org.jahia.modules.ffmailchimp.actions;

import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * Created by stefan on 2017-02-06.
 */
public class SaveConfiguration extends Action {
    private final static Logger logger = LoggerFactory.getLogger(SaveConfiguration.class);

    @Override
    public ActionResult doExecute(HttpServletRequest httpServletRequest, RenderContext renderContext, Resource resource, JCRSessionWrapper session, Map<String, List<String>> map, URLResolver urlResolver) throws Exception {
        ActionResult actionResult = new ActionResult(HttpServletResponse.SC_OK);

        JCRNodeWrapper siteNode = resource.getNode();
        JCRNodeWrapper formFactoryFolder;
        if (!siteNode.hasNode("formFactory")) {
            formFactoryFolder = siteNode.addNode("formFactory", "fcnt:formFactory");
            session.save();
        } else {
            formFactoryFolder = siteNode.getNode("formFactory");
        }
        if (!formFactoryFolder.isNodeType("fcmix:mailchimpConfiguration")) {
            formFactoryFolder.addMixin("fcmix:mailchimpConfiguration");
        }
        JCRNodeWrapper mailchimpConfigurationNode;
        if (!formFactoryFolder.hasNode("mailchimpConfiguration")) {
            mailchimpConfigurationNode = formFactoryFolder.addNode("mailchimpConfiguration", "fcnt:mailchimpConfiguration");
        } else {
            mailchimpConfigurationNode = formFactoryFolder.getNode("mailchimpConfiguration");
        }
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            String key = entry.getKey();
            mailchimpConfigurationNode.setProperty(key, entry.getValue().get(0));
        }
        session.save();
        JSONObject jsonAnswer = new JSONObject();
        jsonAnswer.put("status", "success");
        jsonAnswer.put("message", "Mailchimp configuration was saved successfully!");
        actionResult.setJson(jsonAnswer);
        return actionResult;
    }
}
