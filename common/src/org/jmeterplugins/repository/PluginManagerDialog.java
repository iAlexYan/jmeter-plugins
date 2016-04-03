package org.jmeterplugins.repository;

import kg.apc.jmeter.JMeterPluginsUtils;
import org.apache.jmeter.gui.action.ActionNames;
import org.apache.jmeter.gui.action.ActionRouter;
import org.apache.jorphan.gui.ComponentUtil;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class PluginManagerDialog extends JDialog {
    private static final Logger log = LoggingManager.getLoggerForClass();
    public static final Border SPACING = BorderFactory.createEmptyBorder(5, 5, 5, 5);
    private final PluginManager manager;
    private final JTextArea modifs = new JTextArea();
    private final JButton apply = new JButton("Apply Changes and Restart JMeter");

    public PluginManagerDialog(PluginManager aManager) {
        super((JFrame) null, "Plugins Manager", true);
        setLayout(new BorderLayout());
        this.manager = aManager;
        this.setSize(new Dimension(640, 480));
        this.setIconImage(JMeterPluginsUtils.getIcon().getImage());
        ComponentUtil.centerComponentInWindow(this, 50);

        try {
            this.manager.load();
        } catch (IOException e) {
            e.printStackTrace();
        }

        add(getTabsPanel(), BorderLayout.CENTER);
        add(getBottomPanel(), BorderLayout.SOUTH);
    }


    private Component getTabsPanel() {
        JTabbedPane tabbedPane = new JTabbedPane();

        tabbedPane.addTab("Installed Plugins", getInstalledPane());

        tabbedPane.addTab("Available Plugins", getAvailablePane());
        return tabbedPane;
    }

    private Component getAvailablePane() {
        PluginsList panel = new PluginsList();
        for (Plugin plugin : manager.getPlugins()) {
            if (!plugin.isInstalled()) {
                panel.add(plugin);
            }
        }
        return panel;
    }

    private Component getInstalledPane() {
        PluginsList panel = new PluginsList();
        for (Plugin plugin : manager.getPlugins()) {
            if (plugin.isInstalled()) {
                panel.add(plugin);
            }
        }
        return panel;
    }

    private JPanel getBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel modifsPanel = new JPanel(new BorderLayout());
        modifsPanel.setMinimumSize(new Dimension(200, 200));
        modifsPanel.setBorder(SPACING);
        modifsPanel.setBorder(BorderFactory.createTitledBorder("Review Changes"));

        modifs.setEditable(false);
        modifsPanel.add(modifs, BorderLayout.CENTER);

        panel.add(modifsPanel, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new BorderLayout());
        btnPanel.setBorder(SPACING);
        btnPanel.add(apply, BorderLayout.EAST);
        btnPanel.add(new JPanel(), BorderLayout.CENTER);
        panel.add(btnPanel, BorderLayout.SOUTH);

        apply.addActionListener(new ApplyAction());
        return panel;
    }

    private class ApplyAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent evt) {
            // FIXME: what to do when user presses "cancel" on save test plan dialog?
            Plugin test = manager.getPluginByID("jmeter-tcp");

            final Set<Plugin> deletions = new HashSet<>();
            if (test.isInstalled()) {
                deletions.add(test);
            }

            final Set<Plugin> additions = new HashSet<>();
            if (!test.isInstalled()) {
                try {
                    test.download("2.13");
                    additions.add(test);
                } catch (Exception ex) {
                    log.error("Failed to download " + test, ex);
                }
            }

            manager.modifierHook(deletions, additions);
            // query updates for installed
            ActionRouter.getInstance().actionPerformed(new ActionEvent(this, 0, ActionNames.EXIT));
        }
    }
}
