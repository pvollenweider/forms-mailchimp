package org.jahia.modules.ffmailchimp.actions;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.modules.ffmailchimp.SubmissionMetaData;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * Created by stefan on 2017-02-06.
 */
public class RetrieveLists extends Action{
    private final static Logger logger = LoggerFactory.getLogger(RetrieveLists.class);
    private SchedulerService schedulerService;

    @Override
    public ActionResult doExecute(HttpServletRequest httpServletRequest, RenderContext renderContext, Resource resource, JCRSessionWrapper session, Map<String, List<String>> map, URLResolver urlResolver) throws Exception {
        //Check that we have the key
        JSONObject jsonAnswer = new JSONObject();
        List<String> parameterList = map.get("apiKey");
        if (parameterList.size() > 0) {
            String apiKey = parameterList.get(0);
            String server = apiKey.substring(apiKey.indexOf('-') + 1, apiKey.length());
            StringBuilder entryPointSb = new StringBuilder("https://");
            entryPointSb.append(server).append(".api.mailchimp.com/3.0/lists");
            try {
                HttpResponse<JsonNode> response = Unirest.get(entryPointSb.toString()).basicAuth(null, apiKey).asJson();
                //Prepare object for easy use.
                JSONObject results = response.getBody().getObject();
                JSONObject lists = new JSONObject();
                List<String> backgroundJobListIds = new LinkedList<>();
                if (results != null) {
                    JSONArray rawLists = results.getJSONArray("lists");
                    for (int i = 0; i < rawLists.length(); i++) {
                        JSONObject list = (JSONObject)rawLists.get(i);
                        String listId = list.getString("id");
                        lists.put(listId, list.getString("name"));
                        backgroundJobListIds.add(listId);
                    }
                    //Setup job to check lists for missing merge fields and to add them to the respective list.
                    JobDetail jahiaJob = BackgroundJob.createJahiaJob("Verifying Merge fields in available lists. Any missing fields will be added.", VerifyAndCreateListMergeFields.class);
                    jahiaJob.setName("VerifyAndCreateListMergeFields"+ org.apache.commons.id.uuid.UUID.randomUUID().toString());
                    jahiaJob.setGroup("FFActions");
                    JobDataMap jobDataMap = jahiaJob.getJobDataMap();
                    jobDataMap.put("username", session.getUser().getUserKey());
                    jobDataMap.put("apiKey", apiKey);
                    jobDataMap.put("listIds", backgroundJobListIds);
                    //Update path for Merge Fields
                    entryPointSb.append("/{listId}/merge-fields");
                    jobDataMap.put("apiEntryPoint", entryPointSb.toString());
                    schedulerService.scheduleJobAtEndOfRequest(jahiaJob);
                }
                jsonAnswer.put("lists", lists);
                jsonAnswer.put("status", "success");
                jsonAnswer.put("message", "Retrieved lists successfully");
                return new ActionResult(HttpServletResponse.SC_OK, null, jsonAnswer);
            } catch (UnirestException e) {
                //Removed a saved list from mailchimp configuration (if one is saved already);
                JCRNodeWrapper mailchimpConfigurationNode = resource.getNode().getNode("formFactory/mailchimpConfiguration");
                mailchimpConfigurationNode.setProperty("listId", "");
                session.save();
                jsonAnswer.put("status", "error");
                jsonAnswer.put("message", e.getMessage());
                return new ActionResult(HttpServletResponse.SC_OK, null, jsonAnswer);
            }
        } else {
            jsonAnswer.put("status", "error");
            jsonAnswer.put("message", "Api key is missing");
            return new ActionResult(HttpServletResponse.SC_BAD_REQUEST, null, jsonAnswer);
        }
    }

    public void setSchedulerService(SchedulerService schedulerService) {
        this.schedulerService = schedulerService;
    }
}
