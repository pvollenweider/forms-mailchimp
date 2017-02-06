package org.jahia.modules.ffmailchimp.actions;

import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * Created by stefan on 2017-02-06.
 */
public class RemoveMailchimpConfigurationAction extends Action{
    @Override
    public ActionResult doExecute(HttpServletRequest httpServletRequest, RenderContext renderContext, Resource resource, JCRSessionWrapper session, Map<String, List<String>> map, URLResolver urlResolver) throws Exception {
        ActionResult actionResult = new ActionResult(HttpServletResponse.SC_OK);
        JSONObject jsonAnswer = new JSONObject();
        jsonAnswer.put("success", "Successfully removed Mailchimp configuration!");
        actionResult.setJson(jsonAnswer);

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
        return actionResult;
    }
}
