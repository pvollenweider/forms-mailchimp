package org.jahia.modules.ffmailchimp.actions;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by stefan on 2017-02-06.
 */
public class RetrieveLists extends Action{
    private final static Logger logger = LoggerFactory.getLogger(RetrieveLists.class);

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
                if (results != null) {
                    JSONArray rawLists = results.getJSONArray("lists");
                    //Update path for Merge Fields
                    entryPointSb.append("/{listId}/merge-fields");
                    for (int i = 0; i < rawLists.length(); i++) {
                        JSONObject list = (JSONObject)rawLists.get(i);
                        lists.put(list.getString("id"), list.getString("name"));
                        //Check if we need to add meta data merge fields
                        HttpResponse<JsonNode> mergeFieldsResponse = Unirest.get(entryPointSb.toString())
                                .basicAuth(null, apiKey)
                                .routeParam("listId", list.getString("id"))
                                .asJson();
                        JSONArray mergeFields = mergeFieldsResponse.getBody().getObject().getJSONArray("merge_fields");
                        Map<String, String> mergeFieldExistsMap = new LinkedHashMap<>();
                        mergeFieldExistsMap.put("FFSERVER", "Server Address");
                        mergeFieldExistsMap.put("FFREFERRER", "Referrer");
                        mergeFieldExistsMap.put("FFFORMID", "Form Identifier");
                        //Check which merge fields do not exist on the current list
                        for (int j = 0; j < mergeFields.length(); j++) {
                            String mergeTag = new JSONObject(mergeFields.getString(j)).getString("tag");
                            if (mergeFieldExistsMap.get(mergeTag) != null) {
                                mergeFieldExistsMap.remove(mergeFields.get(j));
                            }
                            if (mergeFieldExistsMap.size() == 0) {
                                break;
                            }
                        }
                        if (mergeFieldExistsMap.size() > 0) {
                            //Add meta data merge fields that don't exist on this list.
                            for (Map.Entry<String, String> entry : mergeFieldExistsMap.entrySet()) {
                                JSONObject reqBody = new JSONObject();
                                reqBody.put("tag", entry.getKey())
                                        .put("name", entry.getValue())
                                        .put("type", "text")
                                        .put("public", false);
                                Unirest.post(entryPointSb.toString())
                                        .basicAuth(null, apiKey)
                                        .header("Content-Type", "application/json")
                                        .routeParam("listId", list.getString("id"))
                                        .body(reqBody.toString())
                                        .asJson();
                            }
                        }
                    }
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
}
