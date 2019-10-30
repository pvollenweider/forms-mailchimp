package org.jahia.modules.forms.mailchimp.actions;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import java.util.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.jahia.services.scheduler.BackgroundJob;
import org.jahia.services.scheduler.SchedulerService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;

/**
 * Created by stefan on 2017-02-06.
 */
public class RetrieveLists extends Action {

    private SchedulerService schedulerService;

    @Override
    public ActionResult doExecute(HttpServletRequest httpServletRequest, RenderContext renderContext, Resource resource, JCRSessionWrapper session, Map<String, List<String>> map, URLResolver urlResolver) throws Exception {
        // Check that we have the key
        final JSONObject jsonAnswer = new JSONObject();
        final List<String> parameterList = map.get("apiKey");
        if (parameterList.isEmpty()) {
            jsonAnswer.put(Constants.ATTR_STATUS, "error");
            jsonAnswer.put(Constants.ATTR_MESSAGE, "Api key is missing");
            return new ActionResult(HttpServletResponse.SC_BAD_REQUEST, null, jsonAnswer);
        } else {
            final String apiKey = parameterList.get(0);
            final String server = apiKey.substring(apiKey.indexOf('-') + 1, apiKey.length());
            final StringBuilder entryPointSb = new StringBuilder(Constants.SCHEME_HTTPS);
            entryPointSb.append(server).append(".api.mailchimp.com/3.0/lists");
            try {
                final HttpResponse<JsonNode> response = Unirest.get(entryPointSb.toString())
                        .basicAuth(null, apiKey)
                        .queryString("fields", Constants.ATTR_LISTS)
                        .queryString("count", "100")
                        .queryString("sort_field", "date_created")
                        .queryString("sort_dir", "DESC")
                        .asJson();
                // Prepare object for easy use.
                final JSONObject results = response.getBody().getObject();
                final JSONObject lists = new JSONObject();
                final List<String> backgroundJobListIds = new LinkedList<>();
                if (results != null) {
                    final JSONArray rawLists = results.getJSONArray(Constants.ATTR_LISTS);
                    for (int i = 0; i < rawLists.length(); i++) {
                        final JSONObject list = (JSONObject) rawLists.get(i);
                        final String listId = list.getString("id");
                        lists.put(listId, list.getString("name"));
                        backgroundJobListIds.add(listId);
                    }
                    // Setup job to check lists for missing merge fields and to add them to the respective list.
                    final JobDetail jahiaJob = BackgroundJob.createJahiaJob("Verifying Merge fields in available lists. Any missing fields will be added.", VerifyAndCreateListMergeFields.class);
                    jahiaJob.setName("VerifyAndCreateListMergeFields" + org.apache.commons.id.uuid.UUID.randomUUID().toString());
                    jahiaJob.setGroup("FFActions");
                    final JobDataMap jobDataMap = jahiaJob.getJobDataMap();
                    jobDataMap.put("username", session.getUser().getUserKey());
                    jobDataMap.put("apiKey", apiKey);
                    jobDataMap.put("listIds", backgroundJobListIds);
                    //Update path for Merge Fields
                    entryPointSb.append("/{listId}/merge-fields");
                    jobDataMap.put("apiEntryPoint", entryPointSb.toString());
                    schedulerService.scheduleJobAtEndOfRequest(jahiaJob);
                }
                jsonAnswer.put(Constants.ATTR_LISTS, lists);
                jsonAnswer.put(Constants.ATTR_STATUS, Constants.VALUE_SUCCESS);
                jsonAnswer.put(Constants.ATTR_MESSAGE, "Retrieved lists successfully");
                return new ActionResult(HttpServletResponse.SC_OK, null, jsonAnswer);
            } catch (UnirestException e) {
                // Removed a saved list from mailchimp configuration (if one is saved already);
                final JCRNodeWrapper mailchimpConfigurationNode = resource.getNode().getNode("formFactory/mailchimpConfiguration");
                mailchimpConfigurationNode.setProperty("listId", "");
                session.save();
                jsonAnswer.put(Constants.ATTR_STATUS, Constants.VALUE_ERROR);
                jsonAnswer.put(Constants.ATTR_MESSAGE, e.getMessage());
                return new ActionResult(HttpServletResponse.SC_OK, null, jsonAnswer);
            }
        }
    }

    public void setSchedulerService(SchedulerService schedulerService) {
        this.schedulerService = schedulerService;
    }
}
