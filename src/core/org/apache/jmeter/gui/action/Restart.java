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
package org.apache.jmeter.gui.action;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.MenuElement;

import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.gui.plugin.MenuCreator;
import org.apache.jmeter.util.JMeterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Restart JMeter
 * Based on https://dzone.com/articles/programmatically-restart-java
 * @since 5.0
 */
public class Restart extends AbstractAction implements MenuCreator {
    private static final Logger log = LoggerFactory.getLogger(Restart.class);

    private static final String RESTART = "restart";
    /** 
     * Sun property pointing the main class and its arguments. 
     * Might not be defined on non Hotspot VM implementations.
     */
    public static final String SUN_JAVA_COMMAND = "sun.java.command";

    private static final Set<String> commands = new HashSet<>();

    static {
        commands.add(RESTART);
    }

    /**
     * @see Command#doAction(ActionEvent)
     */
    @Override
    public void doAction(ActionEvent e) {
        try {
            restartApplication(null);
        } catch (Exception ex) {
            log.error("Error trying to restart: {}", ex.getMessage(), ex);
            JOptionPane.showMessageDialog(GuiPackage.getInstance().getMainFrame(), 
                    JMeterUtils.getResString("restart_error")+":\n" + ex.getLocalizedMessage(),  //$NON-NLS-1$  //$NON-NLS-2$
                    JMeterUtils.getResString("error_title"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$

        }
    }

    /**
     * Restart the current Java application
     * 
     * @param runBeforeRestart
     *            some custom code to be run before restarting
     * @throws Exception
     */
    public static void restartApplication(Runnable runBeforeRestart) throws Exception {
        // java binary
        String java = System.getProperty("java.home") + "/bin/java";
        // vm arguments
        List<String> vmArguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
        StringBuilder vmArgsOneLine = new StringBuilder();
        for (String arg : vmArguments) {
            // if it's the agent argument : we ignore it otherwise the
            // address of the old application and the new one will be in
            // conflict
            if (!arg.contains("-agentlib")) {
                vmArgsOneLine.append(arg);
                vmArgsOneLine.append(" ");
            }
        }
        // init the command to execute, add the vm args
        final StringBuilder cmd = new StringBuilder(java + " " + vmArgsOneLine);

        // program main and program arguments
        String[] mainCommand = System.getProperty(SUN_JAVA_COMMAND).split(" ");
        // program main is a jar
        if (mainCommand[0].endsWith(".jar")) {
            // if it's a jar, add -jar mainJar
            cmd.append("-jar " + new File(mainCommand[0]).getPath());
        } else {
            // else it's a .class, add the classpath and mainClass
            cmd.append("-cp \"" + System.getProperty("java.class.path") + "\" " + mainCommand[0]);
        }
        // finally add program arguments
        for (int i = 1; i < mainCommand.length; i++) {
            cmd.append(" ");
            cmd.append(mainCommand[i]);
        }
        // execute the command in a shutdown hook, to be sure that all the
        // resources have been disposed before restarting the application
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    Runtime.getRuntime().exec(cmd.toString());
                } catch (IOException e) {
                    log.error("Error calling restart command {}", cmd.toString(), e);
                }
            }
        });
        // execute some custom code before restarting
        if (runBeforeRestart != null) {
            runBeforeRestart.run();
        }
        // exit
        System.exit(0);

    }

    /**
     * @see Command#getActionNames()
     */
    @Override
    public Set<String> getActionNames() {
        return commands;
    }

    @Override
    public JMenuItem[] getMenuItemsAtLocation(MENU_LOCATION location) {
        if(location == MENU_LOCATION.FILE) {
            
            JMenuItem menuItemIC = new JMenuItem(
                    JMeterUtils.getResString(RESTART), KeyEvent.VK_UNDEFINED);
            menuItemIC.setName(RESTART);
            menuItemIC.setActionCommand(RESTART);
            menuItemIC.setAccelerator(null);
            menuItemIC.addActionListener(ActionRouter.getInstance());

            return new JMenuItem[]{menuItemIC};
        }
        return new JMenuItem[0];
    }
    /**
     * 
     */
    public Restart() {
        super();
    }

    @Override
    public JMenu[] getTopLevelMenus() {
        return new JMenu[0];
    }

    @Override
    public boolean localeChanged(MenuElement menu) {
        return false;
    }

    @Override
    public void localeChanged() {
        // NOOP
    }
}
