package org.jahia.modules.ffmailchimp.actions;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.commons.lang.StringUtils;
import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * Created by stefan on 2017-02-07.
 */
public class RetrieveListMergeFields extends Action {
    private static final Logger logger = LoggerFactory.getLogger(RetrieveListMergeFields.class);
    @Override
    public ActionResult doExecute(HttpServletRequest req, RenderContext renderContext, Resource resource, JCRSessionWrapper session, Map<String, List<String>> parameters, URLResolver urlResolver) throws Exception {
        ActionResult actionResult = new ActionResult(HttpServletResponse.SC_OK);
        JSONObject jsonAnswer = new JSONObject();
        getMergeFields : try {
            JCRNodeWrapper mailchimpConfiguration = resource.getNode().getNode("formFactory/mailchimpConfiguration");
            String apiKey = mailchimpConfiguration.getPropertyAsString("apiKey");
            String listId = mailchimpConfiguration.getPropertyAsString("listId");
            if (StringUtils.isEmpty(apiKey)) {
                jsonAnswer.put("status", "error");
                jsonAnswer.put("message", "Mailchimp API key is not set");
                jsonAnswer.put("errorType", "missingApiKey");
                break getMergeFields;
            }
            if (StringUtils.isEmpty(listId)) {
                jsonAnswer.put("status", "error");
                jsonAnswer.put("message", "Mailchimp list ID is not set");
                jsonAnswer.put("errorType", "missingListId");
                break getMergeFields;
            }
            String server = apiKey.substring(apiKey.indexOf('-') + 1, apiKey.length());
            StringBuilder entryPointSb = new StringBuilder("https://");
            entryPointSb.append(server).append(".api.mailchimp.com/3.0/lists/{listId}/merge-fields");
            HttpResponse<JsonNode> response = Unirest.get(entryPointSb.toString())
                    .basicAuth(null, apiKey)
                    .routeParam("listId", listId)
                    .asJson();
            JSONArray mergeFields = response.getBody().getObject().getJSONArray("merge_fields");
            jsonAnswer.put("results", mergeFields);
            jsonAnswer.put("status", "success");
            jsonAnswer.put("message", "Successfully retrieved available merge fields");
            logger.info("Successfully retrieved available merge fields");
        } catch (RepositoryException e) {
            jsonAnswer.put("status", "error");
            jsonAnswer.put("errorType", "invalidConfiguration");
            jsonAnswer.put("message", "Mailchimp configuration does not exist");
            logger.error("No mailchimp configuration node found: " + e.getMessage(), e);
        } catch (UnirestException e) {
            jsonAnswer.put("status", "error");
            jsonAnswer.put("message", e.getMessage());
            jsonAnswer.put("errorType", "invalidApiKey");
            logger.error("Request to retrieve merge fields failed: " + e.getMessage(), e);
        } finally {
            actionResult.setJson(jsonAnswer);
            return actionResult;
        }
    }
}