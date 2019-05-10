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
package org.apache.jmeter.assertions;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;

import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.AbstractScopedAssertion;
import org.apache.jmeter.testelement.property.BooleanProperty;
import org.apache.jmeter.testelement.property.StringProperty;
import org.apache.jmeter.util.XPathUtil;

import net.sf.saxon.s9api.SaxonApiException;

/**
 * Checks if the result is a well-formed XML content and whether it matches an
 * XPath
 *
 */
public class XPath2Assertion extends AbstractScopedAssertion implements Serializable, Assertion {
    private static final long serialVersionUID = 241L;
    // + JMX file attributes
    private static final String XPATH_KEY = "XPath.xpath"; // $NON-NLS-1$
    private static final String VALIDATE_KEY = "XPath.validate"; // $NON-NLS-1$
    private static final String NEGATE_KEY = "XPath.negate"; // $NON-NLS-1$
    private static final String NAMESPACES = "XPath2Assertion.namespaces"; // $NON-NLS-1$
    // - JMX file attributes
    public static final String DEFAULT_XPATH = "/";

    /**
     * Returns the result of the Assertion. Checks if the result is well-formed XML,
     * and that the XPath expression is matched (or not, as the case may be)
     */
    @Override
    public AssertionResult getResult(SampleResult response) {
        // no error as default
        AssertionResult result = new AssertionResult(getName());
        result.setFailure(false);
        result.setFailureMessage("");
        byte[] responseData = null;
        if (isScopeVariable()) {
            String inputString = getThreadContext().getVariables().get(getVariableName());
            if (!StringUtils.isEmpty(inputString)) {
                responseData = inputString.getBytes(StandardCharsets.UTF_8);
            }
        } else {
            responseData = response.getResponseData();
        }
        if (responseData == null || responseData.length == 0) {
            return result.setResultForNull();
        }
        try {
            XPathUtil.computeAssertionResultUsingSaxon(result, new String(responseData), getXPathString(),
                    getNamespaces());
        } catch (SaxonApiException e) {
            result.setError(true);
            result.setFailureMessage("SAXException: " + e.getMessage());
            return result;
        }
        return result;
    }

    /**
     * Get The XPath String that will be used in matching the document
     *
     * @return String xpath String
     */
    public String getXPathString() {
        return getPropertyAsString(XPATH_KEY, DEFAULT_XPATH);
    }

    /**
     * Set the XPath String this will be used as an xpath
     *
     * @param xpath String
     */
    public void setXPathString(String xpath) {
        setProperty(new StringProperty(XPATH_KEY, xpath));
    }

    /**
     * Set use validation
     *
     * @param validate Flag whether validation should be used
     */
    public void setValidating(boolean validate) {
        setProperty(new BooleanProperty(VALIDATE_KEY, validate));
    }

    public void setNegated(boolean negate) {
        setProperty(new BooleanProperty(NEGATE_KEY, negate));
    }

    /**
     * Is this validating
     *
     * @return boolean
     */
    public boolean isValidating() {
        return getPropertyAsBoolean(VALIDATE_KEY, false);
    }

    /**
     * Negate the XPath test, that is return true if something is not found.
     *
     * @return boolean negated
     */
    public boolean isNegated() {
        return getPropertyAsBoolean(NEGATE_KEY, false);
    }

    public void setNamespaces(String namespaces) {
        setProperty(NAMESPACES, namespaces);
    }

    public String getNamespaces() {
        return getPropertyAsString(NAMESPACES);
    }
}
