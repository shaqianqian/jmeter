/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.apache.jmeter.extractor.json.jsonpath;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.processor.PostProcessor;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.AbstractScopedTestElement;
import org.apache.jmeter.testelement.ThreadListener;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.jmeter.util.JMeterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import io.burt.jmespath.Expression;

/**
 * JMESPATH based extractor
 * 
 * @since 5.0
 */
public class JMESExtractor extends AbstractScopedTestElement implements Serializable, PostProcessor, ThreadListener {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(JMESExtractor.class);
    private static final String JSON_PATH_EXPRESSION = "JMESExtractor.jsonPathExpr"; // $NON-NLS-1$
    private static final String REFERENCE_NAME = "JMESExtractor.referenceName"; // $NON-NLS-1$
    private static final String DEFAULT_VALUE = "JMESExtractor.defaultValue"; // $NON-NLS-1$
    private static final String MATCH_NUMBER = "JMESExtractor.match_number"; // $NON-NLS-1$
    private static final String REF_MATCH_NR = "_matchNr"; // $NON-NLS-1$
    private static final LoadingCache<String, Expression<JsonNode>> JMES_EXTRACTOR_CACHE;
    static {
        final int cacheSize = JMeterUtils.getPropDefault("JMESExtractor.parser.cache.size", 400);
        JMES_EXTRACTOR_CACHE = Caffeine.newBuilder().maximumSize(cacheSize).build(new JMESCacheLoader());
    }

    @Override
    public void process() {
        JMeterContext context = getThreadContext();
        JMeterVariables vars = context.getVariables();
        String jsonResponse;
        if (isScopeVariable()) {
            jsonResponse = vars.get(getVariableName());
            if (log.isDebugEnabled()) {
                log.debug("JSON Extractor is using variable: {}, which content is: {}", getVariableName(),
                        jsonResponse);
            }
        } else {
            SampleResult previousResult = context.getPreviousResult();
            if (previousResult == null) {
                return;
            }
            jsonResponse = previousResult.getResponseDataAsString();
            if (log.isDebugEnabled()) {
                log.debug("JSON Extractor {} working on Response: {}", getName(), jsonResponse);
            }
        }
        String refName = getRefName();
        String defaultValue = getDefaultValue();
        int matchNumber = Integer.parseInt(getMatchNumber());
        final String jsonPathExpression = getJsonPathExpression().trim();
        clearOldRefVars(vars, refName);
        try {
            if (StringUtils.isEmpty(jsonResponse)) {
                if (log.isDebugEnabled()) {
                    log.debug("Response or source variable is null or empty for {}", getName());
                }
                vars.put(refName, defaultValue);
            } else {
                JsonNode result = null;
                ObjectMapper mapper = new ObjectMapper();
                JsonNode actualObj = mapper.readValue(jsonResponse, JsonNode.class);
                result = JMES_EXTRACTOR_CACHE.get(jsonPathExpression).search(actualObj);
                if (result.isNull()) {
                    vars.put(refName, defaultValue);
                    vars.put(refName + REF_MATCH_NR, "0"); //$NON-NLS-1$
                } else {
                    if (matchNumber < 0) {
                        String extractedString = stringify(result);
                        vars.put(refName + "_" + 1, extractedString); // $NON-NLS-1$
                    } else if (matchNumber == 0) {
                        placeObjectIntoVars(vars, refName, result);
                    } else {
                        if (matchNumber > 1) {
                            if (log.isDebugEnabled()) {
                                log.debug(
                                        "matchNumber({}) exceeds number of items found({}), default value will be used",
                                        matchNumber, 1);
                            }
                            vars.put(refName, defaultValue);
                        } else {
                            placeObjectIntoVars(vars, refName, result);
                        }
                    }
                    if (matchNumber != 0) {
                        vars.put(refName + REF_MATCH_NR, Integer.toString(1));
                    }
                }
            }
        } catch (Exception e) {
            // if something wrong, default value added
            if (log.isDebugEnabled()) {
                log.debug("Error processing JSON content in {}, message: {}", getName(), e.getLocalizedMessage(), e);
            } else {
                log.debug("Error processing JSON content in {}, message: {}", getName(), e.getLocalizedMessage());
            }
            vars.put(refName, defaultValue);
        }
    }

    private void clearOldRefVars(JMeterVariables vars, String refName) {
        vars.remove(refName + REF_MATCH_NR);
        for (int i = 1; vars.get(refName + "_" + i) != null; i++) {
            vars.remove(refName + "_" + i);
        }
    }

    private void placeObjectIntoVars(JMeterVariables vars, String currentRefName, Object extractedValue) {
        vars.put(currentRefName, stringify(extractedValue));
    }

    private String stringify(Object obj) {
        return obj == null ? "" : obj.toString(); //$NON-NLS-1$
    }

    public String getJsonPathExpression() {
        return getPropertyAsString(JSON_PATH_EXPRESSION);
    }

    public void setJsonPathExpression(String jsonPath) {
        setProperty(JSON_PATH_EXPRESSION, jsonPath);
    }

    public String getRefName() {
        return getPropertyAsString(REFERENCE_NAME);
    }

    public void setRefName(String refName) {
        setProperty(REFERENCE_NAME, refName);
    }

    public String getDefaultValue() {
        return getPropertyAsString(DEFAULT_VALUE);
    }

    public void setDefaultValue(String defaultValue) {
        setProperty(DEFAULT_VALUE, defaultValue, ""); // $NON-NLS-1$
    }

    @Override
    public void threadStarted() {
        // NOOP
    }

    @Override
    public void threadFinished() {
        JMES_EXTRACTOR_CACHE.cleanUp();
    }

    public void setMatchNumbers(String matchNumber) {
        setProperty(MATCH_NUMBER, matchNumber);
    }

    public String getMatchNumber() {
        return getPropertyAsString(MATCH_NUMBER);
    }
}
