/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 */
package org.apache.jmeter.visualizers;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.Timer;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

import org.apache.jmeter.config.NfrArgument;
import org.apache.jmeter.gui.GUIMenuSortOrder;
import org.apache.jmeter.gui.util.HeaderAsPropertyRenderer;
import org.apache.jmeter.gui.util.TextAreaCellRenderer;
import org.apache.jmeter.gui.util.TextAreaTableCellEditor;
import org.apache.jmeter.samplers.Clearable;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jmeter.visualizers.gui.AbstractVisualizer;
import org.apache.jorphan.gui.GuiUtils;
import org.apache.jorphan.gui.ObjectTableModel;
import org.apache.jorphan.reflect.Functor;

/**
 * Aggregate Table-Based Reporting Visualizer for JMeter.
 */
@GUIMenuSortOrder(3)
public class NfrListnerGui extends AbstractVisualizer implements Clearable, ActionListener {
    private static final long serialVersionUID = 242L;
    private static final String USE_GROUP_NAME = "useGroupName"; //$NON-NLS-1$
    private static final String SAVE_HEADERS = "saveHeaders"; //$NON-NLS-1$
    private static final String TOTAL_ROW_LABEL = JMeterUtils.getResString("aggregate_report_total_label"); //$NON-NLS-1$
    private static final int REFRESH_PERIOD = JMeterUtils.getPropDefault("jmeter.gui.refresh_period", 500); // $NON-NLS-1$
    private final JButton saveTable = new JButton(JMeterUtils.getResString("aggregate_graph_save_table")); //$NON-NLS-1$
    private final JCheckBox saveHeaders = new JCheckBox(JMeterUtils.getResString("aggregate_graph_save_table_header"), //$NON-NLS-1$
            true);
    private final JCheckBox useGroupName = new JCheckBox(JMeterUtils.getResString("aggregate_graph_use_group_name")); //$NON-NLS-1$
    private transient ObjectTableModel model;
    /** Lock used to protect tableRows update + model update */
    private final transient Object lock = new Object();
    public static Map<String, SamplingStatCalculator> tableRows = new ConcurrentHashMap<>();
    private Deque<SamplingStatCalculator> newRows = new ConcurrentLinkedDeque<>();
    private volatile boolean dataChanged;
    private final JButton add = new JButton("add"); //$NON-NLS-1$
    private final JButton delete = new JButton("delete"); //$NON-NLS-1$

    public NfrListnerGui() {
        super();
        model = StatGraphVisualizer.createObjectTableModel();
        clearData();
        init();
    }

    /**
     * @return <code>true</code> iff all functors can be found
     * @deprecated - only for use in testing
     */
    @Deprecated
    public static boolean testFunctors() {
        NfrListnerGui instance = new NfrListnerGui();
        return instance.model.checkFunctors(null, instance.getClass());
    }

    @Override
    public String getLabelResource() {
        return "non_function_test"; //$NON-NLS-1$
    }

    @Override
    public void add(final SampleResult res) {
        SamplingStatCalculator row = tableRows.computeIfAbsent(res.getSampleLabel(useGroupName.isSelected()), label -> {
            SamplingStatCalculator newRow = new SamplingStatCalculator(label);
            newRows.add(newRow);
            return newRow;
        });
        synchronized (row) {
            /*
             * Synch is needed because multiple threads can update the counts.
             */
            row.addSample(res);
        }
        SamplingStatCalculator tot = tableRows.get(TOTAL_ROW_LABEL);
        synchronized (lock) {
            tot.addSample(res);
        }
        dataChanged = true;
    }

    /**
     * Clears this visualizer and its model, and forces a repaint of the table.
     */
    @Override
    public void clearData() {
        synchronized (lock) {
            model.clearData();
            tableRows.clear();
            newRows.clear();
            tableRows.put(TOTAL_ROW_LABEL, new SamplingStatCalculator(TOTAL_ROW_LABEL));
            model.addRow(tableRows.get(TOTAL_ROW_LABEL));
        }
    }

    /**
     * Main visualizer setup.
     */
    private void init() { // WARNING: called from ctor so must not be overridden (i.e. must be private or
                          // final)
        this.setLayout(new BorderLayout());
        // MAIN PANEL
        JPanel mainPanel = new JPanel();
        Border margin = new EmptyBorder(10, 10, 5, 10);
        mainPanel.setBorder(margin);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.add(makeTitlePanel());
        this.add(mainPanel, BorderLayout.NORTH);
        this.add(createStringPanel(), BorderLayout.CENTER);
////////////////////////////////////////////////////////////////        
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        add.addActionListener(new AddPatternListener());
        delete.addActionListener(new ClearPatternsListener());
        buttonPanel.add(add);
        buttonPanel.add(delete);
        add.setEnabled(true);
        this.add(buttonPanel, BorderLayout.SOUTH);
//////////////////////////////////////////////////////////////// 
        new Timer(REFRESH_PERIOD, e -> {
            if (!dataChanged) {
                return;
            }
            dataChanged = false;
            synchronized (lock) {
                while (!newRows.isEmpty()) {
                    model.insertRow(newRows.pop(), model.getRowCount() - 1);
                }
            }
            model.fireTableDataChanged();
        }).start();
    }
    /**
     * An ActionListener for deleting a pattern.
     *
     */
    private class ClearPatternsListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            GuiUtils.cancelEditing(stringTable);

            int[] rowsSelected = stringTable.getSelectedRows();
            stringTable.clearSelection();
            if (rowsSelected.length > 0) {
                for (int i = rowsSelected.length - 1; i >= 0; i--) {
                    tableModel.removeRow(rowsSelected[i]);
                }
                tableModel.fireTableDataChanged();
            } else {
                if(tableModel.getRowCount()>0) {
                    tableModel.removeRow(0);
                    tableModel.fireTableDataChanged();
                }
            }

            if (stringTable.getModel().getRowCount() == 0) {
                delete.setEnabled(false);
            }
        }
    }
    /**
     * An ActionListener for adding a pattern.
     */
    private class AddPatternListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            // If a table cell is being edited, we should accept the current value
            // and stop the editing before adding a new row.
            GuiUtils.stopTableEditing(stringTable);

            tableModel.addRow( new NfrArgument("", "","",""));

            checkButtonsStatus();

            // Highlight (select) and scroll to the appropriate row.
            int rowToSelect = tableModel.getRowCount() - 1;
            stringTable.setRowSelectionInterval(rowToSelect, rowToSelect);
            stringTable.scrollRectToVisible(stringTable.getCellRect(rowToSelect, 0, true));
        }
    }
    protected void checkButtonsStatus() {
        // Disable DELETE if there are no rows in the table to delete.
        if (tableModel.getRowCount() == 0) {
            delete.setEnabled(false);
        } else {
            delete.setEnabled(true);
        }
    }
    @Override
    public void modifyTestElement(TestElement c) {
        super.modifyTestElement(c);
    }

    @Override
    public void configure(TestElement el) {
        super.configure(el);
    }

    @Override
    public void actionPerformed(ActionEvent ev) {

    }
    /**
     * Create a panel allowing the user to supply a list of string patterns to
     * test against.
     *
     * @return a new panel for adding string patterns
     */
    /** A table of patterns to test against. */
    private JTable stringTable;

    /** Table model for the pattern table. */
    private ObjectTableModel tableModel;
    private JScrollPane createStringPanel() {
        tableModel = new ObjectTableModel(new String[] { "Name", "Criteria" ,"Symbol","Value"},
                NfrArgument.class,
                new Functor[] {
                new Functor("getName"), // $NON-NLS-1$
                new Functor("getCriteria"),
                new Functor("getSymbol"),
                new Functor("getValue")},  // $NON-NLS-1$
                new Functor[] {
                new Functor("setName"), // $NON-NLS-1$
                new Functor("setCriteria"),
                new Functor("setSymbol"),
                new Functor("setValue") }, // $NON-NLS-1$
                new Class[] { String.class, String.class , String.class , String.class });
        stringTable = new JTable(tableModel);
        stringTable.getTableHeader().setDefaultRenderer(new HeaderAsPropertyRenderer());
        stringTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JMeterUtils.applyHiDPI(stringTable);


        TextAreaCellRenderer renderer = new TextAreaCellRenderer();
        stringTable.setRowHeight(renderer.getPreferredHeight());
        stringTable.setDefaultRenderer(String.class, renderer);
        stringTable.setDefaultEditor(String.class, new TextAreaTableCellEditor());
        stringTable.setPreferredScrollableViewportSize(new Dimension(100, 70));
        return new JScrollPane(stringTable);

    }


}
