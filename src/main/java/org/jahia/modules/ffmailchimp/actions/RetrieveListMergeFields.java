package org.jahia.modules.ffmailchimp.actions;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;
import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.modules.ffmailchimp.SubmissionMetaData;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by stefan on 2017-02-07.
 */
public class RetrieveListMergeFields extends Action {

    private static final Logger LOGGER = LoggerFactory.getLogger(RetrieveListMergeFields.class);
    
    @Override
    public ActionResult doExecute(HttpServletRequest req, RenderContext renderContext, Resource resource, JCRSessionWrapper session, Map<String, List<String>> parameters, URLResolver urlResolver) throws Exception {
        ActionResult actionResult;
        final JSONObject jsonAnswer = new JSONObject();
        try {
            if (resource.getNode().getNode(Constants.NODE_FORM_FACTORY).hasNode(Constants.NODE_MAILCHIMP_CONFIGURATION)) {
                actionResult = new ActionResult(HttpServletResponse.SC_OK);
                final JCRNodeWrapper mailchimpConfiguration = resource.getNode().getNode("formFactory/mailchimpConfiguration");
                final String apiKey = mailchimpConfiguration.getPropertyAsString("apiKey");
                final String listId = mailchimpConfiguration.getPropertyAsString("listId");
                if (StringUtils.isEmpty(apiKey)) {
                    actionResult = new ActionResult(HttpServletResponse.SC_BAD_REQUEST);
                    jsonAnswer.put(Constants.ATTR_STATUS, Constants.VALUE_ERROR);
                    jsonAnswer.put(Constants.ATTR_MESSAGE, "Mailchimp API key is not set");
                    jsonAnswer.put(Constants.ATTR_ERROR_TYPE, "missingApiKey");
                    actionResult.setJson(jsonAnswer);
                    return actionResult;
                }
                if (StringUtils.isEmpty(listId)) {
                    actionResult = new ActionResult(HttpServletResponse.SC_BAD_REQUEST);
                    jsonAnswer.put(Constants.ATTR_STATUS, Constants.VALUE_ERROR);
                    jsonAnswer.put(Constants.ATTR_MESSAGE, "Mailchimp list ID is not set");
                    jsonAnswer.put(Constants.ATTR_ERROR_TYPE, "missingListId");
                    actionResult.setJson(jsonAnswer);
                    return actionResult;
                }
                final String server = apiKey.substring(apiKey.indexOf('-') + 1, apiKey.length());
                final StringBuilder entryPointSb = new StringBuilder(Constants.SCHEME_HTTPS);
                entryPointSb.append(server).append(".api.mailchimp.com/3.0/lists/{listId}/merge-fields");
                final HttpResponse<JsonNode> response = Unirest.get(entryPointSb.toString())
                        .basicAuth(null, apiKey)
                        .queryString("fields", "merge_fields")
                        .queryString("count", 30)
                        .routeParam("listId", listId)
                        .asJson();
                final JSONArray mergeFields = response.getBody().getObject().getJSONArray("merge_fields");
                final Set<String> submissionMetaDataValues = SubmissionMetaData.getEnums();
                final JSONArray filteredMergeFields = new JSONArray();
                for (int i = 0; i < mergeFields.length(); i++) {
                    final String mergeTag = ((JSONObject) mergeFields.get(i)).getString("tag");
                    if (!submissionMetaDataValues.contains(mergeTag)) {
                        filteredMergeFields.put(mergeFields.get(i));

                    }
                }
                jsonAnswer.put("results", filteredMergeFields);
                jsonAnswer.put(Constants.ATTR_STATUS, Constants.VALUE_SUCCESS);
                jsonAnswer.put(Constants.ATTR_MESSAGE, "Successfully retrieved available mailchimp merge fields");
                actionResult.setJson(jsonAnswer);
                LOGGER.info("Successfully retrieved available merge fields");
                return actionResult;
            } else {
                actionResult = new ActionResult(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                jsonAnswer.put(Constants.ATTR_STATUS, Constants.VALUE_ERROR);
                jsonAnswer.put(Constants.ATTR_ERROR_TYPE, Constants.VALUE_INVALID_CONFIGURATION);
                jsonAnswer.put(Constants.ATTR_MESSAGE, "Mailchimp configuration does not exist");
                actionResult.setJson(jsonAnswer);
                return actionResult;
            }
        } catch (RepositoryException e) {
            actionResult = new ActionResult(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            jsonAnswer.put(Constants.ATTR_STATUS, Constants.VALUE_ERROR);
            jsonAnswer.put(Constants.ATTR_ERROR_TYPE, Constants.VALUE_INVALID_CONFIGURATION);
            jsonAnswer.put(Constants.ATTR_MESSAGE, "Mailchimp configuration does not exist");
            actionResult.setJson(jsonAnswer);
            LOGGER.warn("No mailchimp configuration node found: " + e.getMessage());
            return actionResult;
        } catch (UnirestException e) {
            actionResult = new ActionResult(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            jsonAnswer.put(Constants.ATTR_STATUS, Constants.VALUE_ERROR);
            jsonAnswer.put(Constants.ATTR_MESSAGE, e.getMessage());
            jsonAnswer.put(Constants.ATTR_ERROR_TYPE, "invalidApiKey");
            actionResult.setJson(jsonAnswer);
            LOGGER.warn("Request to retrieve mailchimp merge fields failed: " + e.getMessage());
            return actionResult;
        }
    }
}
