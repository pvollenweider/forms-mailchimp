package org.jahia.modules.ffmailchimp.actions;

import com.drew.lang.StringUtil;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.StringUtils;
import org.jahia.api.Constants;
import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.modules.ffmailchimp.SubmissionMetaData;
import org.jahia.services.content.*;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by stefan on 2017-02-08.
 */
public class SubscribeToMailchimp extends Action {
    private static final Logger logger = LoggerFactory.getLogger(SubscribeToMailchimp.class);

    @Override
    public ActionResult doExecute(HttpServletRequest req, RenderContext renderContext, Resource resource, JCRSessionWrapper session, Map<String, List<String>> parameters, URLResolver urlResolver) throws Exception {
        ActionResult actionResult = new ActionResult(HttpServletResponse.SC_OK);
        JSONObject jsonAnswer = new JSONObject();
        //Get mailchimp configuration
        JCRNodeWrapper formFactoryNode = renderContext.getSite().getNode("formFactory");
        JCRNodeWrapper mailchimpConfiguration;
        JSONObject mailchimpMergeFields = new JSONObject();
        Map<String, String> inputResults = new LinkedHashMap<>();
        if (formFactoryNode.isNodeType("fcmix:mailchimpConfiguration")) {
            mailchimpConfiguration = formFactoryNode.getNode("mailchimpConfiguration");
            String apiKey = mailchimpConfiguration.getPropertyAsString("apiKey");
            String listId = mailchimpConfiguration.getPropertyAsString("listId");
            String formId = parameters.get("formId").get(0);
            JCRNodeWrapper formNode = session.getNodeByIdentifier(formId);
            List<JCRNodeWrapper> stepNodes = JCRContentUtils.getChildrenOfType(formNode, "fcnt:step");
            JCRNodeWrapper actionNode = resource.getNode();
            String emailInput = actionNode.getNode("mappedEmailInput").getPropertyAsString("jsonValue");
            String email = null;
            for (JCRNodeWrapper step : stepNodes) {
                for (Map.Entry<String, List<String>> entry : parameters.entrySet()) {
                    String inputName = entry.getKey();
                    if (step.hasNode(inputName)) {
                        JCRNodeWrapper input = step.getNode(inputName);
                        boolean isMergeField = false;
                        List values = entry.getValue();
                        if (values.size() == 1) {
                            String value = values.get(0).toString();
                            //Check if this is a json string
                            try {
                                JSONObject jsonObject = new JSONObject(value);
                                //Since there are few inputs that store complex data we will try to figure out
                                //which this is

                                //Check if this is a country input
                                if (jsonObject.has("country")) {
                                    //Country input
                                   value = jsonObject.getJSONObject("country").getString("name");
                                } else if (jsonObject.has("value")) {
                                    //Input with value
                                    value = jsonObject.getString("value");
                                }
                            } catch(JSONException ex) {
                                logger.warn("Failed to read value from json object. Entire object will be sent. Ignore if value is not of \"{ ... }\" type: " + value);
                            }
                            if (input.hasNode("miscDirectives")) {
                                JCRNodeWrapper miscDirectives = input.getNode("miscDirectives");
                                if (miscDirectives.hasNode("mailchimp-mapper")) {
                                    JCRNodeWrapper miscDirective = miscDirectives.getNode("mailchimp-mapper");
                                    if (miscDirective.getPropertyAsString("j:nodename").equals("mailchimp-mapper")) {
                                        mailchimpMergeFields.put(miscDirective.getNode("tag").getPropertyAsString("jsonValue"), value);
                                        isMergeField = true;
                                    }
                                }
                            }
                            if (!StringUtils.isEmpty(value) || isMergeField) {
                                inputResults.put(input.getPropertyAsString("j:nodename"), value);
                            }
                            if (inputName.equals(emailInput)) {
                                email = value;
                            }
                        }
                    }
                }
            }
            if (!StringUtils.isEmpty(email)) {
                //Add enabled Meta data merge fields
                Map<SubmissionMetaData, String> mergeFieldExistsMap = SubmissionMetaData.getSubmissionMetaDataTypesAsMap();
                for (Map.Entry<SubmissionMetaData, String> entry : mergeFieldExistsMap.entrySet()) {
                    SubmissionMetaData submissionMetaData = entry.getKey();
                    String mergeTag = submissionMetaData.toString();
                    if (mailchimpConfiguration.hasProperty(submissionMetaData.getJcrPropertyName()) && mailchimpConfiguration.getProperty(submissionMetaData.getJcrPropertyName()).getBoolean()) {
                        switch (submissionMetaData) {
                            case FFSERVER:
                                mailchimpMergeFields.put(mergeTag, formNode.getResolveSite().getServerName());
                                break;
                            case FFREFERRER:
                                String origin = req.getHeader("referer");
                                mailchimpMergeFields.put(mergeTag, StringUtils.isNotEmpty(origin) ? origin : req.getRequestURI());
                                break;
                            case FFFORMID:
                                mailchimpMergeFields.put(mergeTag, formNode.getPropertyAsString(Constants.JCR_TITLE));
                                break;
                        }
                    }
                }
                String formDisplayId = parameters.get("formDisplayId").get(0);

                //Update subscribe/update member in mailchimp
                byte[] emailAsBytes = email.getBytes("UTF-8");
                MessageDigest md = MessageDigest.getInstance("MD5");
                String emailMD5Hash = Hex.encodeHexString(md.digest(emailAsBytes));
                String server = apiKey.substring(apiKey.indexOf('-') + 1, apiKey.length());
                StringBuilder entryPointSb = new StringBuilder("https://");
                entryPointSb.append(server).append(".api.mailchimp.com/3.0/lists/{listId}/members/{subscriberHash}");
                JSONObject reqBody = new JSONObject();
                reqBody.put("email_address", email)
                        .put("status_if_new", "subscribed")
                        .put("merge_fields", mailchimpMergeFields)
                        .put("ip_signup", req.getRemoteAddr())
                        .put("language", session.getLocale());
                if (session.getNodeByIdentifier(formDisplayId).isNodeType("fcmix:mailchimpGroup")) {
                    JSONObject interests = new JSONObject();
                    JCRValueWrapper[] group = session.getNodeByIdentifier(formDisplayId).getProperty("group").getValues();
                    for (JCRValueWrapper valueWrapper : group) {
                        interests.put(valueWrapper.getString(), Boolean.TRUE);
                    }
                    reqBody.put("interests", interests);
                }
                try {
                    HttpResponse<JsonNode> response = Unirest.put(entryPointSb.toString())
                            .basicAuth(null, apiKey)
                            .header("Content-Type", "application/json")
                            .routeParam("listId", listId)
                            .routeParam("subscriberHash", emailMD5Hash)
                            .body(reqBody.toString())
                            .asJson();
                    jsonAnswer.put("status", "success");
                    jsonAnswer.put("actionName", "subscribeToMailchimp");
                    JSONObject results = new JSONObject();
                    results.put("response", response.getBody().getObject());
                    results.put("submission", inputResults);
                    jsonAnswer.put("results", results);
                    jsonAnswer.put("message", "Subscribed/Updated user to Mailchimp successfully!");
                    jsonAnswer.put("code", HttpServletResponse.SC_OK);
                    actionResult.setJson(results);
                } catch (UnirestException e) {
                    jsonAnswer.put("status", "error");
                    jsonAnswer.put("message", e.getMessage());
                    jsonAnswer.put("code", HttpServletResponse.SC_BAD_REQUEST);
                    actionResult.setResultCode(HttpServletResponse.SC_BAD_REQUEST);
                }
            } else {
                jsonAnswer.put("status", "error");
                jsonAnswer.put("message", "Mailchimp user email was not mapped correctly or is missing.");
                jsonAnswer.put("code", HttpServletResponse.SC_BAD_REQUEST);
                logger.error("Failed to execute Subscribe to Mailchimp due to: empty email field or mapping mismatch");
                actionResult.setResultCode(HttpServletResponse.SC_BAD_REQUEST);
            }
        } else {
            jsonAnswer.put("status", "error");
            jsonAnswer.put("message", "Mailchimp configuration does not exist on this site");
            jsonAnswer.put("code", HttpServletResponse.SC_BAD_REQUEST);
            logger.error("Failed to execute Subscribe to Mailchimp due to missing mailchimp configuration");
            actionResult.setResultCode(HttpServletResponse.SC_BAD_REQUEST);
        }
        actionResult.setJson(jsonAnswer);
        return actionResult;
    }
}
