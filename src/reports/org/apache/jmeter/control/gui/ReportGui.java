//$Header:
/*
 * Copyright 2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

package org.apache.jmeter.control.gui;

import java.awt.Color;
import java.awt.BorderLayout;
import java.awt.Container;
import java.util.Collection;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.config.gui.ArgumentsPanel;
import org.apache.jmeter.gui.util.ReportMenuFactory;
import org.apache.jmeter.report.gui.AbstractReportGui;
import org.apache.jmeter.report.gui.ReportPageGui;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.ReportPlan;
import org.apache.jmeter.util.JMeterUtils;

/**
 * JMeter GUI component representing the test plan which will be executed when
 * the test is run.
 * 
 * @version $Revision$ Last Updated: $Date$
 */
public class ReportGui extends AbstractReportGui {

	private JCheckBox serializedMode;

	/** A panel allowing the user to define variables. */
	private ArgumentsPanel argsPanel;

	/** A panel to contain comments on the test plan. */
	private JTextField commentPanel;

	/**
	 * Create a new TestPlanGui.
	 */
	public ReportGui() {
		init();
	}

	/**
	 * Need to update this to make the context popupmenu correct
	 * @return a JPopupMenu appropriate for the component.
	 */
	public JPopupMenu createPopupMenu() {
		JPopupMenu pop = new JPopupMenu();
		JMenu addMenu = new JMenu(JMeterUtils.getResString("Add"));
		addMenu.add(ReportMenuFactory.makeMenuItem(new ReportPageGui().getStaticLabel(), ReportPageGui.class.getName(),
				"Add"));
		addMenu.add(ReportMenuFactory.makeMenu(ReportMenuFactory.CONFIG_ELEMENTS, "Add"));
		pop.add(addMenu);
		ReportMenuFactory.addFileMenu(pop);
		return pop;
	}

	/* Implements JMeterGUIComponent.createTestElement() */
	public TestElement createTestElement() {
		ReportPlan tp = new ReportPlan();
		modifyTestElement(tp);
		return tp;
	}

	/* Implements JMeterGUIComponent.modifyTestElement(TestElement) */
	public void modifyTestElement(TestElement plan) {
		super.configureTestElement(plan);
		if (plan instanceof ReportPlan) {
			ReportPlan tp = (ReportPlan) plan;
			tp.setUserDefinedVariables((Arguments) argsPanel.createTestElement());
			tp.setProperty(ReportPlan.COMMENTS, commentPanel.getText());
		}
	}

	public String getLabelResource() {
		return "report_plan";
	}

	/**
	 * This is the list of menu categories this gui component will be available
	 * under. This implementation returns null, since the TestPlan appears at
	 * the top level of the tree and cannot be added elsewhere.
	 * 
	 * @return a Collection of Strings, where each element is one of the
	 *         constants defined in MenuFactory
	 */
	public Collection getMenuCategories() {
		return null;
	}

	/**
	 * A newly created component can be initialized with the contents of a Test
	 * Element object by calling this method. The component is responsible for
	 * querying the Test Element object for the relevant information to display
	 * in its GUI.
	 * 
	 * @param el
	 *            the TestElement to configure
	 */
	public void configure(TestElement el) {
		super.configure(el);

		if (el.getProperty(ReportPlan.USER_DEFINED_VARIABLES) != null) {
			argsPanel.configure((Arguments) el.getProperty(ReportPlan.USER_DEFINED_VARIABLES).getObjectValue());
		}
		commentPanel.setText(el.getPropertyAsString(ReportPlan.COMMENTS));
	}

	/**
	 * Create a panel allowing the user to define variables for the test.
	 * 
	 * @return a panel for user-defined variables
	 */
	private JPanel createVariablePanel() {
		argsPanel = 
            new ArgumentsPanel(JMeterUtils.getResString("user_defined_variables"),
                    Color.white);
		return argsPanel;
	}

	private Container createCommentPanel() {
		Container panel = makeTitlePanel();
		commentPanel = new JTextField();
        commentPanel.setBackground(Color.white);
		JLabel label = new JLabel(JMeterUtils.getResString("testplan_comments"));
        label.setBackground(Color.white);
		label.setLabelFor(commentPanel);
		panel.add(label);
		panel.add(commentPanel);
		return panel;
	}

	/**
	 * Initialize the components and layout of this component.
	 */
	private void init() {
		setLayout(new BorderLayout(10, 10));
		setBorder(makeBorder());
        setBackground(Color.white);
		add(createCommentPanel(), BorderLayout.NORTH);
		add(createVariablePanel(), BorderLayout.CENTER);
	}
}
