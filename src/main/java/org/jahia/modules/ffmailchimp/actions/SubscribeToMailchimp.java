package org.jahia.modules.ffmailchimp.actions;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.jcr.NodeIterator;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.StringUtils;
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

/**
 * Created by stefan on 2017-02-08.
 */
public class SubscribeToMailchimp extends Action {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubscribeToMailchimp.class);

    @Override
    public ActionResult doExecute(HttpServletRequest req, RenderContext renderContext, Resource resource, JCRSessionWrapper session, Map<String, List<String>> parameters, URLResolver urlResolver) throws Exception {
        final ActionResult actionResult = new ActionResult(HttpServletResponse.SC_OK);
        final JSONObject jsonAnswer = new JSONObject();
        // Get mailchimp configuration
        final JCRNodeWrapper formFactoryNode = renderContext.getSite().getNode(Constants.NODE_FORM_FACTORY);
        final JCRNodeWrapper mailchimpConfiguration;
        final JSONObject mailchimpMergeFields = new JSONObject();
        final Map<String, String> inputResults = new LinkedHashMap<>();
        if (formFactoryNode.isNodeType(Constants.MIX_MAILCHIMP_CONFIGURATION)) {
            mailchimpConfiguration = formFactoryNode.getNode(Constants.NODE_MAILCHIMP_CONFIGURATION);
            final String apiKey = mailchimpConfiguration.getPropertyAsString("apiKey");
            final String listId = mailchimpConfiguration.getPropertyAsString("listId");
            final String formId = parameters.get("formId").get(0);
            final JCRNodeWrapper formNode = session.getNodeByIdentifier(formId);
            final List<JCRNodeWrapper> stepNodes = JCRContentUtils.getChildrenOfType(formNode, "fcnt:step");
            final NodeIterator childrenOfType = JCRContentUtils.getDescendantNodes(formNode, "fcnt:passwordDefinition");
            final List<String> passordInputName = new ArrayList<>();
            while (childrenOfType.hasNext()) {
                final JCRNodeWrapper next = (JCRNodeWrapper) childrenOfType.next();
                passordInputName.add(next.getName());
            }
            final JCRNodeWrapper actionNode = resource.getNode();
            final String emailInput = actionNode.getNode("mappedEmailInput").getPropertyAsString("jsonValue");
            String email = null;
            for (JCRNodeWrapper step : stepNodes) {
                for (Map.Entry<String, List<String>> entry : parameters.entrySet()) {
                    final String inputName = entry.getKey();
                    if (step.hasNode(inputName) && !passordInputName.contains(inputName)) {
                        final JCRNodeWrapper input = step.getNode(inputName);
                        boolean isMergeField = false;
                        final List values = entry.getValue();
                        if (values.size() == 1) {
                            String value = values.get(0).toString();
                            // Check if this is a json string
                            try {
                                final JSONObject jsonObject = new JSONObject(value);
                                // Since there are few inputs that store complex data we will try to figure out
                                // which this is

                                // Check if this is a country input
                                if (jsonObject.has("country")) {
                                    // Country input
                                    value = jsonObject.getJSONObject("country").getString("name");
                                } else if (jsonObject.has("value")) {
                                    // Input with value
                                    value = jsonObject.getString("value");
                                }
                            } catch (JSONException ex) {
                                final String errMsg = "Failed to read value from json object for field %s. Entire object will be sent.";
                                LOGGER.warn(String.format(errMsg, inputName));
                            }
                            if (input.hasNode("miscDirectives")) {
                                final JCRNodeWrapper miscDirectives = input.getNode("miscDirectives");
                                if (miscDirectives.hasNode(Constants.NODE_MAILCHIMP_MAPPER)) {
                                    JCRNodeWrapper miscDirective = miscDirectives.getNode(Constants.NODE_MAILCHIMP_MAPPER);
                                    if (miscDirective.getPropertyAsString("j:nodename").equals(Constants.NODE_MAILCHIMP_MAPPER)) {
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
                // Add enabled Meta data merge fields
                final Map<SubmissionMetaData, String> mergeFieldExistsMap = SubmissionMetaData.getSubmissionMetaDataTypesAsMap();
                for (Map.Entry<SubmissionMetaData, String> entry : mergeFieldExistsMap.entrySet()) {
                    final SubmissionMetaData submissionMetaData = entry.getKey();
                    final String mergeTag = submissionMetaData.toString();
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
                                mailchimpMergeFields.put(mergeTag, formNode.getPropertyAsString(org.jahia.api.Constants.JCR_TITLE));
                                break;
                        }
                    }
                }
                final String formDisplayId = parameters.get("formDisplayId").get(0);

                // Update subscribe/update member in mailchimp
                final byte[] emailAsBytes = email.getBytes("UTF-8");
                final MessageDigest md = MessageDigest.getInstance("MD5");
                final String emailMD5Hash = Hex.encodeHexString(md.digest(emailAsBytes));
                final String server = apiKey.substring(apiKey.indexOf('-') + 1, apiKey.length());
                final StringBuilder entryPointSb = new StringBuilder(Constants.SCHEME_HTTPS);
                entryPointSb.append(server).append(".api.mailchimp.com/3.0/lists/{listId}/members/{subscriberHash}");
                final JSONObject reqBody = new JSONObject();
                reqBody.put("email_address", email)
                        .put("status_if_new", "subscribed")
                        .put("merge_fields", mailchimpMergeFields)
                        .put("ip_signup", req.getRemoteAddr())
                        .put("language", session.getLocale());
                if (session.getNodeByIdentifier(formDisplayId).isNodeType("fcmix:mailchimpGroup")) {
                    final JSONObject interests = new JSONObject();
                    final JCRValueWrapper[] group = session.getNodeByIdentifier(formDisplayId).getProperty("group").getValues();
                    for (JCRValueWrapper valueWrapper : group) {
                        interests.put(valueWrapper.getString(), Boolean.TRUE);
                    }
                    reqBody.put("interests", interests);
                }
                try {
                    final HttpResponse<JsonNode> response = Unirest.put(entryPointSb.toString())
                            .basicAuth(null, apiKey)
                            .header("Content-Type", "application/json")
                            .routeParam("listId", listId)
                            .routeParam("subscriberHash", emailMD5Hash)
                            .body(reqBody.toString())
                            .asJson();
                    jsonAnswer.put(Constants.ATTR_STATUS, Constants.VALUE_SUCCESS);
                    jsonAnswer.put("actionName", "subscribeToMailchimp");
                    final JSONObject results = new JSONObject();
                    results.put("response", response.getBody().getObject());
                    results.put("submission", inputResults);
                    jsonAnswer.put("results", results);
                    jsonAnswer.put(Constants.ATTR_MESSAGE, "Subscribed/Updated user to Mailchimp successfully!");
                    jsonAnswer.put(Constants.ATTR_CODE, HttpServletResponse.SC_OK);
                    LOGGER.info("Subscribe to mailchimp responded with code (" + response.getStatus() + "): " + response.getStatusText());
                    LOGGER.info("response body: " + response.getBody());
                    actionResult.setJson(results);
                } catch (UnirestException e) {
                    jsonAnswer.put(Constants.ATTR_STATUS, Constants.VALUE_SUCCESS);
                    jsonAnswer.put(Constants.ATTR_MESSAGE, e.getMessage());
                    jsonAnswer.put(Constants.ATTR_CODE, HttpServletResponse.SC_BAD_REQUEST);
                    actionResult.setResultCode(HttpServletResponse.SC_BAD_REQUEST);
                }
            } else {
                jsonAnswer.put(Constants.ATTR_STATUS, Constants.VALUE_ERROR);
                jsonAnswer.put(Constants.ATTR_MESSAGE, "Mailchimp user email was not mapped correctly or is missing.");
                jsonAnswer.put(Constants.ATTR_CODE, HttpServletResponse.SC_BAD_REQUEST);
                LOGGER.error("Failed to execute Subscribe to Mailchimp due to: empty email field or mapping mismatch");
                actionResult.setResultCode(HttpServletResponse.SC_BAD_REQUEST);
            }
        } else {
            jsonAnswer.put(Constants.ATTR_STATUS, Constants.VALUE_ERROR);
            jsonAnswer.put(Constants.ATTR_MESSAGE, "Mailchimp configuration does not exist on this site");
            jsonAnswer.put(Constants.ATTR_CODE, HttpServletResponse.SC_BAD_REQUEST);
            LOGGER.error("Failed to execute Subscribe to Mailchimp due to missing mailchimp configuration");
            actionResult.setResultCode(HttpServletResponse.SC_BAD_REQUEST);
        }
        actionResult.setJson(jsonAnswer);
        return actionResult;
    }
}
