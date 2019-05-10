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

package org.apache.jmeter.assertions.gui;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.jmeter.assertions.XPath2Assertion;
import org.apache.jmeter.gui.GUIMenuSortOrder;
import org.apache.jmeter.gui.util.JSyntaxTextArea;
import org.apache.jmeter.gui.util.JTextScrollPane;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.util.JMeterUtils;

@GUIMenuSortOrder(50)
public class XPath2AssertionGui extends AbstractAssertionGui { // $NOSONAR

    private static final long serialVersionUID = 240L;// $NON-NLS-1$
    private XPathPanel xpath;
    private JSyntaxTextArea namespacesTA;
    
    public XPath2AssertionGui() {
        super();
        init();
    }

    /**
     * Returns the label to be shown within the JTree-Component.
     */
    @Override
    public String getLabelResource() {
        return "xpath2_assertion_title"; //$NON-NLS-1$
    }

    /**
     * Create test element
     */
    @Override
    public TestElement createTestElement() {
        XPath2Assertion el = new XPath2Assertion();
        modifyTestElement(el);
        return el;
    }

    public String getXPathAttributesTitle() {
        return JMeterUtils.getResString("xpath2_assertion_test"); //$NON-NLS-1$
    }

    @Override
    public void configure(TestElement el) {
        super.configure(el);
        XPath2Assertion assertion = (XPath2Assertion) el;
        showScopeSettings(assertion, true);
        xpath.setXPath(assertion.getXPathString());
        xpath.setNegated(assertion.isNegated());
        namespacesTA.setText(assertion.getNamespaces());
    }

    private void init() { // WARNING: called from ctor so must not be overridden (i.e. must be private or final)
        setLayout(new BorderLayout());
        setBorder(makeBorder());

        Box topBox = Box.createVerticalBox();

        topBox.add(makeTitlePanel());

        topBox.add(createScopePanel(true));
        topBox.add(makeParameterPanel());
        add(topBox, BorderLayout.NORTH);

        // USER_INPUT
        JPanel sizePanel = new JPanel(new BorderLayout());
        sizePanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        sizePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
                getXPathAttributesTitle()));
        xpath = new XPathPanel();
        sizePanel.add(xpath);
        add(sizePanel, BorderLayout.CENTER);
    }
    private JPanel makeParameterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        initConstraints(gbc);     
        panel.add(new JLabel(JMeterUtils.getResString("xpath_extractor_user_namespaces")), gbc.clone());
        gbc.gridx++;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        namespacesTA = JSyntaxTextArea.getInstance(5, 80);
        panel.add(JTextScrollPane.getInstance(namespacesTA, true), gbc.clone());
        resetContraints(gbc);
        
        return panel;
    }
    /**
     * Modifies a given TestElement to mirror the data in the gui components.
     *
     * @see org.apache.jmeter.gui.JMeterGUIComponent#modifyTestElement(TestElement)
     */
    @Override
    public void modifyTestElement(TestElement el) {
        super.configureTestElement(el);
        if (el instanceof XPath2Assertion) {
            XPath2Assertion assertion = (XPath2Assertion) el;
            saveScopeSettings(assertion);
            assertion.setNegated(xpath.isNegated());
            assertion.setXPathString(xpath.getXPath());
            assertion.setNamespaces(namespacesTA.getText());
        }
    }

    /**
     * Implements JMeterGUIComponent.clearGui
     */
    @Override
    public void clearGui() {
        super.clearGui();
        xpath.setXPath("/"); //$NON-NLS-1$
        xpath.setNegated(false);
        namespacesTA.setText("");

    }
    private void resetContraints(GridBagConstraints gbc) {
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
    }

    private void initConstraints(GridBagConstraints gbc) {
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridheight = 1;
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.weighty = 0;
    }
    
}
