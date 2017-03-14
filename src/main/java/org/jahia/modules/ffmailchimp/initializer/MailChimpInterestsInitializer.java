package org.jahia.modules.ffmailchimp.initializer;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.content.nodetypes.ExtendedPropertyDefinition;
import org.jahia.services.content.nodetypes.initializers.ChoiceListValue;
import org.jahia.services.content.nodetypes.initializers.ModuleChoiceListInitializer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by rincevent on 2017-03-09.
 */
public class MailChimpInterestsInitializer implements ModuleChoiceListInitializer {
    @Override
    public void setKey(String s) {

    }

    @Override
    public String getKey() {
        return "mailchimp";
    }

    @Override
    public List<ChoiceListValue> getChoiceListValues(ExtendedPropertyDefinition extendedPropertyDefinition, String s, List<ChoiceListValue> list, Locale locale, Map<String, Object> map) {
        List<ChoiceListValue> results = new ArrayList<>();
        JCRNodeWrapper nodeWrapper = (JCRNodeWrapper) map.get("contextNode");
        if(nodeWrapper==null) {
            nodeWrapper = (JCRNodeWrapper) map.get("contextParent");
        }
        try {
            JCRSiteNode resolveSite = nodeWrapper.getResolveSite();
            if (resolveSite.hasNode("formFactory") && resolveSite.getNode("formFactory").isNodeType("fcmix:mailchimpConfiguration")) {
                JCRNodeWrapper mailchimpConfiguration = resolveSite.getNode("formFactory").getNode("mailchimpConfiguration");
                String apiKey = mailchimpConfiguration.getPropertyAsString("apiKey");
                String listId = mailchimpConfiguration.getPropertyAsString("listId");
                String server = apiKey.substring(apiKey.indexOf('-') + 1, apiKey.length());
                String url = "https://" + server + ".api.mailchimp.com/3.0/lists/" + listId + "/interest-categories";
                //Until release of Hotfixes
                HttpResponse<JsonNode> response = Unirest.get(url)
                        .basicAuth(null, apiKey)
                        .queryString("count", "100")
                        .asJson();
                //Prepare object for easy use.
                JSONObject jsonObject = response.getBody().getObject();
                if (jsonObject != null) {
                    JSONArray rawLists = jsonObject.getJSONArray("categories");
                    for (int i = 0; i < rawLists.length(); i++) {
                        JSONObject category = (JSONObject) rawLists.get(i);
                        String mainCategoryName = category.getString("title");
                        HttpResponse<JsonNode> subCategories = Unirest.get(url + "/" + category.get("id") + "/interests")
                                .basicAuth(null, apiKey)
                                .queryString("count", "100")
                                .asJson();
                        JSONObject jsonObject2 = subCategories.getBody().getObject();
                        if (jsonObject2 != null) {
                            JSONArray rawLists2 = jsonObject2.getJSONArray("interests");
                            for (int j = 0; j < rawLists2.length(); j++) {
                                JSONObject subCategory = (JSONObject) rawLists2.get(j);
                                results.add(new ChoiceListValue(mainCategoryName + " - " + subCategory.getString("name") + " (" + subCategory.getInt("subscriber_count") + " subscriber(s))", subCategory.getString("id")));
                            }
                        }
                    }
                }

                /*if (extendedPropertyDefinition.getName().equals("category")) {
                    HttpResponse<JsonNode> response = Unirest.get(url)
                            .basicAuth(null, apiKey)
                            .queryString("count", "100")
                            .asJson();
                    //Prepare object for easy use.
                    JSONObject jsonObject = response.getBody().getObject();
                    if (jsonObject != null) {
                        JSONArray rawLists = jsonObject.getJSONArray("categories");
                        for (int i = 0; i < rawLists.length(); i++) {
                            JSONObject category = (JSONObject) rawLists.get(i);
                            String mainCategoryName = category.getString("title");
                            results.add(new ChoiceListValue(mainCategoryName, category.getString("id")));
                        }
                    }
                } else if(map.containsKey("category")){
                    HttpResponse<JsonNode> subCategories = Unirest.get(url + "/" + ((String) ((List) map.get("category")).get(0)) + "/interests")
                            .basicAuth(null, apiKey)
                            .queryString("count", "100")
                            .asJson();
                    JSONObject jsonObject2 = subCategories.getBody().getObject();
                    if (jsonObject2 != null) {
                        JSONArray rawLists2 = jsonObject2.getJSONArray("interests");
                        for (int j = 0; j < rawLists2.length(); j++) {
                            JSONObject subCategory = (JSONObject) rawLists2.get(j);
                            results.add(new ChoiceListValue(subCategory.getString("name") + " (" + subCategory.getInt("subscriber_count") + " subscriber(s))", subCategory.getString("id")));
                        }
                    }
                } else if(nodeWrapper.isNodeType("fcmix:mailchimpGroup") && nodeWrapper.hasProperty("category")){
                    HttpResponse<JsonNode> subCategories = Unirest.get(url + "/" + nodeWrapper.getProperty("category").getString() + "/interests")
                            .basicAuth(null, apiKey)
                            .queryString("count", "100")
                            .asJson();
                    JSONObject jsonObject2 = subCategories.getBody().getObject();
                    if (jsonObject2 != null) {
                        JSONArray rawLists2 = jsonObject2.getJSONArray("interests");
                        for (int j = 0; j < rawLists2.length(); j++) {
                            JSONObject subCategory = (JSONObject) rawLists2.get(j);
                            results.add(new ChoiceListValue(subCategory.getString("name") + " (" + subCategory.getInt("subscriber_count") + " subscriber(s))", subCategory.getString("id")));
                        }
                    }
                }*/
            }
        } catch (UnirestException e) {
            e.printStackTrace();
        } catch (PathNotFoundException e) {
            e.printStackTrace();
        } catch (RepositoryException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return results;
    }
}
