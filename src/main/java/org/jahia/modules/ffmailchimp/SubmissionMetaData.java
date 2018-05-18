package org.jahia.modules.ffmailchimp;

import org.apache.pdfbox.pdmodel.graphics.predictor.Sub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by stefan on 2017-02-13.
 */
public enum SubmissionMetaData {
    FFSERVER("Server Address", "server"),
    FFREFERRER("Referrer", "referrer"),
    FFFORMID("Form Identifier", "formId");

    private static final Logger logger = LoggerFactory.getLogger(SubmissionMetaData.class);
    private final String displayName;
    private final String jcrPropertyName;
    private static HashSet<String> values;

    SubmissionMetaData(String displayName, String jcrPropertyName) {
        this.displayName = displayName;
        this.jcrPropertyName = jcrPropertyName;
    }

    public static Map<SubmissionMetaData, String> getSubmissionMetaDataTypesAsMap() {
        Map<SubmissionMetaData, String> map = new LinkedHashMap<>();
        for (SubmissionMetaData submissionMetaData : SubmissionMetaData.values()) {
            map.put(submissionMetaData, submissionMetaData.displayName);
        }
        return map;
    }

    public static HashSet<String> getEnums() {
        if (values == null) {
            values = new HashSet<>();
            for (SubmissionMetaData submissionMetaData : SubmissionMetaData.values()) {
                values.add(submissionMetaData.name());
            }
        }
        return values;
    }

    public String getJcrPropertyName() {
        return this.jcrPropertyName;
    }
}
