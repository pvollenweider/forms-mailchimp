package org.jahia.modules.ffmailchimp.actions;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.jahia.modules.ffmailchimp.SubmissionMetaData;
import org.jahia.services.scheduler.BackgroundJob;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by stefan on 2017-02-13.
 */
public class VerifyAndCreateListMergeFields extends BackgroundJob {

    private static final Logger logger = LoggerFactory.getLogger(VerifyAndCreateListMergeFields.class);

    @Override
    public void executeJahiaJob(JobExecutionContext jobExecutionContext) throws JSONException {
        final JobDataMap jobDataMap = jobExecutionContext.getJobDetail().getJobDataMap();
        String apiKey = jobDataMap.getString("apiKey");
        String apiEntryPoint = jobDataMap.getString("apiEntryPoint");
        List<String> listIds = (LinkedList) jobDataMap.get("listIds");
        HashSet<String> submissionMetaDataValues = SubmissionMetaData.getEnums();
        try {
            for (String listId : listIds) {
                //Check if we need to add meta data merge fields
                HttpResponse<JsonNode> mergeFieldsResponse = Unirest.get(apiEntryPoint)
                        .basicAuth(null, apiKey)
                        .routeParam("listId", listId)
                        .asJson();
                JSONArray mergeFields = mergeFieldsResponse.getBody().getObject().getJSONArray("merge_fields");
                Map<SubmissionMetaData, String> mergeFieldExistsMap = SubmissionMetaData.getSubmissionMetaDataTypesAsMap();
                //Check which merge fields do not exist on the current list
                for (int j = 0; j < mergeFields.length(); j++) {
                    String mergeTag = new JSONObject(mergeFields.getString(j)).getString("tag");
                    if (submissionMetaDataValues.contains(mergeTag)) {
                        mergeFieldExistsMap.remove(SubmissionMetaData.valueOf(mergeTag));
                    }
                    if (mergeFieldExistsMap.size() == 0) {
                        break;
                    }
                }
                if (mergeFieldExistsMap.size() > 0) {
                    //Add meta data merge fields that don't exist on this list.
                    for (Map.Entry<SubmissionMetaData, String> entry : mergeFieldExistsMap.entrySet()) {
                        JSONObject reqBody = new JSONObject();
                        reqBody.put("tag", entry.getKey().toString())
                                .put("name", entry.getValue())
                                .put("type", "text")
                                .put("public", false);
                        Unirest.post(apiEntryPoint.toString())
                                .basicAuth(null, apiKey)
                                .header("Content-Type", "application/json")
                                .routeParam("listId", listId)
                                .body(reqBody.toString())
                                .asJson();
                    }
                }
            }
            jobDataMap.put(BackgroundJob.JOB_STATUS, BackgroundJob.STATUS_SUCCESSFUL);
        } catch (UnirestException e) {
            //Removed a saved list from mailchimp configuration (if one is saved already);
            logger.error("Failed to create merge field", e.getMessage());
            jobDataMap.put(BackgroundJob.JOB_STATUS, BackgroundJob.STATUS_FAILED);
        }
    }
}
