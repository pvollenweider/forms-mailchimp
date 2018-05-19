package org.jahia.modules.ffmailchimp.initializer;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.jcr.RepositoryException;
import org.jahia.modules.ffmailchimp.actions.Constants;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.content.nodetypes.ExtendedPropertyDefinition;
import org.jahia.services.content.nodetypes.initializers.ChoiceListValue;
import org.jahia.services.content.nodetypes.initializers.ModuleChoiceListInitializer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by rincevent on 2017-03-09.
 */
public class MailChimpInterestsInitializer implements ModuleChoiceListInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(MailChimpInterestsInitializer.class);

    @Override
    public void setKey(String s) {

    }

    @Override
    public String getKey() {
        return "mailchimp";
    }

    @Override
    public List<ChoiceListValue> getChoiceListValues(ExtendedPropertyDefinition extendedPropertyDefinition, String s, List<ChoiceListValue> list, Locale locale, Map<String, Object> map) {
        final List<ChoiceListValue> results = new ArrayList<>();
        JCRNodeWrapper nodeWrapper = (JCRNodeWrapper) map.get("contextNode");
        if (nodeWrapper == null) {
            nodeWrapper = (JCRNodeWrapper) map.get("contextParent");
        }
        try {
            final JCRSiteNode resolveSite = nodeWrapper.getResolveSite();
            if (resolveSite.hasNode(Constants.NODE_FORM_FACTORY) && resolveSite.getNode(Constants.NODE_FORM_FACTORY).isNodeType(Constants.MIX_MAILCHIMP_CONFIGURATION)) {
                final JCRNodeWrapper mailchimpConfiguration = resolveSite.getNode(Constants.NODE_FORM_FACTORY).getNode(Constants.NODE_MAILCHIMP_CONFIGURATION);
                final String apiKey = mailchimpConfiguration.getPropertyAsString("apiKey");
                final String listId = mailchimpConfiguration.getPropertyAsString("listId");
                final String server = apiKey.substring(apiKey.indexOf('-') + 1, apiKey.length());
                final String url = Constants.SCHEME_HTTPS + server + ".api.mailchimp.com/3.0/lists/" + listId + "/interest-categories";
                // Until release of Hotfixes
                final HttpResponse<JsonNode> response = Unirest.get(url)
                        .basicAuth(null, apiKey)
                        .queryString("count", "100")
                        .asJson();
                // Prepare object for easy use.
                final JSONObject jsonObject = response.getBody().getObject();
                if (jsonObject != null) {
                    final JSONArray rawLists = jsonObject.getJSONArray("categories");
                    for (int i = 0; i < rawLists.length(); i++) {
                        final JSONObject category = (JSONObject) rawLists.get(i);
                        final String mainCategoryName = category.getString("title");
                        final HttpResponse<JsonNode> subCategories = Unirest.get(url + "/" + category.get("id") + "/interests")
                                .basicAuth(null, apiKey)
                                .queryString("count", "100")
                                .asJson();
                        final JSONObject jsonObject2 = subCategories.getBody().getObject();
                        if (jsonObject2 != null) {
                            final JSONArray rawLists2 = jsonObject2.getJSONArray("interests");
                            for (int j = 0; j < rawLists2.length(); j++) {
                                final JSONObject subCategory = (JSONObject) rawLists2.get(j);
                                results.add(new ChoiceListValue(mainCategoryName + " - " + subCategory.getString("name") + " (" + subCategory.getInt("subscriber_count") + " subscriber(s))", subCategory.getString("id")));
                            }
                        }
                    }
                }
            }
        } catch (UnirestException | RepositoryException | JSONException e) {
            LOGGER.error("Impossible to get choice list values", s);
        }
        return results;
    }
}
