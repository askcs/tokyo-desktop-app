package com.askcs.tokyodesktopclient;

import java.awt.AWTException;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Image;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import com.almende.eve.agent.AgentHost;
import com.almende.eve.rpc.jsonrpc.JSONRPCException;
import com.almende.eve.scheduler.RunnableSchedulerFactory;
import com.almende.eve.state.FileStateFactory;
import com.almende.eve.transport.xmpp.XmppService;
import com.askcs.commons.entity.SensorState;
import com.askcs.tokyodesktopclient.agents.ActivityMonitorAgent;
import com.askcs.tokyodesktopclient.event.BusProvider;
import com.askcs.tokyodesktopclient.event.PcActivitySensorEvent;
import com.askcs.tokyodesktopclient.event.UnsupportedPlatformEvent;
import com.google.common.eventbus.Subscribe;

public class MainPcGui {

    private JFrame frame;
    private JTextField usernameTextField;
    private JPasswordField passwordField;
    private JButton btnLogin;
    private JButton btnLogout;
    private SystemTray tray;
    private TrayIcon trayIcon;
    private boolean opening = false;
    private AgentHost host;
    private JLabel lblPcactivity;
    private JLabel lblPcactivitystate;
    private JLabel lblUsername;
    private JLabel lblPassword;
    private Image loggedInImage;
    private Image inactiveImage;
    private Image loggedOutImage;
    private static final String ACTIVITY_MONITOR_AGENT_RESOURCE = "activityMonitor";

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    MainPcGui window = new MainPcGui();
                    window.frame.setVisible(true);
                } catch (Exception e) {
                    System.out.println("failed to setup gui");
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Create the application.
     */
    public MainPcGui() {
        setupAgentHost();
        initialize();
        BusProvider.getBus().register(this);
        recoverSession();
    }

    /**
     * Creates agent and logs into the xmpp server with the provided
     * credentials, removes agent when xmpp login or creation fails
     * 
     * @param username
     * @param password
     */
    private void login(final String username, final String password) {
        setGuiLoginState();
        ActivityMonitorAgent activityMonitorAgent = null;
        try {
            activityMonitorAgent = (ActivityMonitorAgent) host
                    .getAgent(ACTIVITY_MONITOR_AGENT_RESOURCE);
            if (activityMonitorAgent == null) {
                activityMonitorAgent = host.createAgent(ActivityMonitorAgent.class,
                        ACTIVITY_MONITOR_AGENT_RESOURCE);
            }
        } catch (Exception e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        try {
            activityMonitorAgent.setAccount(username, password, ACTIVITY_MONITOR_AGENT_RESOURCE);
            activityMonitorAgent.connect();
            activityMonitorAgent.startAutoMonitor(5000);
            setGuiLoggedInState();
        } catch (Exception e1) {
            JOptionPane.showMessageDialog(frame, "Failed to login", "Login Error",
                    JOptionPane.ERROR_MESSAGE);
            e1.printStackTrace();
            logOut();
        }
    }

    /**
     * logout the monitorAgent
     */
    private void logOut() {
        setGuiloggedOutState();
        ActivityMonitorAgent activityMonitorAgent = null;
        try {
            activityMonitorAgent = (ActivityMonitorAgent) host
                    .getAgent(ACTIVITY_MONITOR_AGENT_RESOURCE);
            if (activityMonitorAgent != null) {
                try {
                    activityMonitorAgent.purge();
                } catch (Exception e) {
                    System.out.println("Failed to purge");
                }
            }
        } catch (Exception e1) {
            System.out.println("Failed to get agent");

        }
    }

    /**
     * Receives PcActivitySensorEvents and changes the gui accordingly
     * 
     * @param event
     */
    @Subscribe
    public void onPcActivityStateEvent(PcActivitySensorEvent event) {
        SensorState sensorState = event.getSensorState();

        if(sensorState.equals(SensorState.AVAILABLE)){
                lblPcactivitystate.setText("available");
            if (tray != null) {
                trayIcon.setImage(loggedInImage);
            }
        }else if(sensorState.equals(SensorState.UNAVAILABLE)){
                lblPcactivitystate.setText("unavailable");
                if (tray != null) {
                trayIcon.setImage(inactiveImage);
                }
        }else{
                lblPcactivitystate.setText("unknown");
                if (tray != null) {
                trayIcon.setImage(loggedOutImage);
                }
        }
    }

    /**
     * Receives PcActivitySensorEvents and changes the gui accordingly
     * 
     * @param event
     */
    @Subscribe
    public void onUnsupportedPlatformEvent(UnsupportedPlatformEvent event) {
        logOut();
        JOptionPane.showMessageDialog(frame, "The current platform is unsupported",
                "Activity Monitor Error",
                JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Restore the gui state based on a the existence of and state of the agent
     * 
     */
    private void recoverSession() {
        boolean hasAgent;
        try {
            hasAgent = host.hasAgent(ACTIVITY_MONITOR_AGENT_RESOURCE);
        } catch (JSONRPCException e1) {
            // failed to get agent assume no agent available
            hasAgent = false;
        }

        if (hasAgent) {
            try {
                // Restore gui state
                setGuiLoggedInState();
                ActivityMonitorAgent activityMonitorAgent = (ActivityMonitorAgent) host
                        .getAgent(ACTIVITY_MONITOR_AGENT_RESOURCE);
                SensorState storedState = activityMonitorAgent.getStoredSensorState();
                onPcActivityStateEvent(new PcActivitySensorEvent(storedState));
                String username = activityMonitorAgent.getUsername();
                String password = activityMonitorAgent.getPassword();
                usernameTextField.setText(username);
                passwordField.setText(password);
            } catch (Exception e) {
                // something is wrong with the existing agent delete agent
                host.deleteAgent(ACTIVITY_MONITOR_AGENT_RESOURCE);
                setGuiloggedOutState();
            }
            } else {
                setGuiloggedOutState();
            }

    }

    /**
     * setGui fields during the login process
     */
    private void setGuiLoginState() {
        passwordField.setEnabled(false);
        usernameTextField.setEnabled(false);
        btnLogin.setEnabled(false);
    }

    /**
     * setGui fields for when the user is logged in
     */
    private void setGuiLoggedInState() {
        usernameTextField.setEnabled(false);
        passwordField.setEnabled(false);
        btnLogin.setEnabled(false);
        btnLogout.setEnabled(true);
        lblPcactivity.setEnabled(true);
        lblPcactivitystate.setEnabled(true);
        if (tray != null) {
        trayIcon.setImage(loggedInImage);
        }
    }

    /**
     * setGui fields for when the user is logged out
     */
    private void setGuiloggedOutState() {
        passwordField.setEnabled(true);
        usernameTextField.setEnabled(true);
        btnLogin.setEnabled(true);
        btnLogout.setEnabled(false);
        lblPcactivity.setEnabled(false);
        lblPcactivitystate.setEnabled(false);
        lblPcactivitystate.setText("");
        if (tray != null) {
        trayIcon.setImage(loggedOutImage);
        }
    }

    /**
     * configure the agentHost.
     */
    private void setupAgentHost() {
        host = AgentHost.getInstance();
        if (host.getStateFactory() == null) {
            host.setStateFactory(new FileStateFactory(".eveagents_tokyo", false));
        host.addTransportService(new XmppService(host, "xmpp.ask-cs.com", 5222, "xmpp.ask-cs.com"));
            host.setSchedulerFactory(new RunnableSchedulerFactory(host, "_myScheduler"));
        }

    }

    /**
     * Initialize the contents of the frame.
     */
    private void initialize() {
        // get trayIcon graphics
        loggedOutImage = new ImageIcon(this.getClass().getResource("/images/disconnected.png"))
                .getImage();
        loggedInImage = new ImageIcon(this.getClass().getResource("/images/favicon.png"))
                .getImage();
        inactiveImage = new ImageIcon(this.getClass().getResource("/images/inactive.png"))
                .getImage();

        frame = new JFrame();
        frame.setBounds(0, -37, 280, 220);
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setIconImage(loggedInImage);
        frame.setTitle("Tokyo desktop client");
        if (SystemTray.isSupported()) {
            tray = SystemTray.getSystemTray();

            ActionListener openFromTray = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    // Deiconify the frame
                    frame.setVisible(true);
                    frame.setExtendedState(JFrame.NORMAL);
                }
            };
            trayIcon = new TrayIcon(loggedInImage, "Tokyo desktop client");
            trayIcon.setImageAutoSize(true);
            trayIcon.addActionListener(openFromTray);

            try {
                tray.add(trayIcon);
            } catch (AWTException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        } else {
            System.out.println("system tray not supported");
        }
        frame.addWindowListener(new WindowListener() {

            public void windowOpened(WindowEvent e) {
                opening = false;

            }

            public void windowIconified(WindowEvent e) {
                if (!opening && tray != null) {
                    frame.setVisible(false);
                }

            }

            public void windowDeiconified(WindowEvent e) {
                frame.setVisible(true);
                opening = true;

            }

            public void windowDeactivated(WindowEvent e) {
                // unused event

            }

            public void windowClosing(WindowEvent e) {
                ActivityMonitorAgent activityMonitorAgent = null;
                try {
                    activityMonitorAgent = (ActivityMonitorAgent) host
                            .getAgent(ACTIVITY_MONITOR_AGENT_RESOURCE);
                } catch (Exception e1) {
                    // Failed to get agent
                }

                if (activityMonitorAgent != null) {
                    // Program closing PcActivityState unavailable
                    activityMonitorAgent.sendSensorState(SensorState.UNKNOWN);
                    activityMonitorAgent.setStoredSensorState(SensorState.UNKNOWN);
                }
                BusProvider.getBus().unregister(this);

            }

            public void windowClosed(WindowEvent e) {
                // unused event

            }

            public void windowActivated(WindowEvent e) {
                opening = false;

            }
        });

        lblUsername = new JLabel("username:");
        lblUsername.setBounds(13, 58, 76, 15);

        usernameTextField = new JTextField();
        usernameTextField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                passwordField.requestFocusInWindow();
            }
        });
        usernameTextField.setFont(new Font("Dialog", Font.PLAIN, 12));
        usernameTextField.setBounds(101, 53, 161, 26);
        usernameTextField.setColumns(10);

        lblPassword = new JLabel("password:");
        lblPassword.setBounds(13, 95, 75, 15);

        btnLogin = new JButton("login");
        btnLogin.setBounds(13, 128, 249, 25);
        btnLogin.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                // Retrieve the login information from the fields and use them
                // to login
                String username = usernameTextField.getText();
                String password = String.valueOf(passwordField.getPassword());
                login(username, password);
                btnLogout.requestFocusInWindow();
            }
        });

        passwordField = new JPasswordField();
        passwordField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                btnLogin.requestFocusInWindow();
            }
        });
        passwordField.setBounds(101, 90, 161, 26);

        btnLogout = new JButton("logout");
        btnLogout.setBounds(13, 155, 249, 25);
        btnLogout.setEnabled(true);
        btnLogout.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                logOut();
            }
        });
        frame.getContentPane().setLayout(null);
        frame.getContentPane().add(lblPassword);
        frame.getContentPane().add(passwordField);
        frame.getContentPane().add(btnLogin);
        frame.getContentPane().add(btnLogout);
        frame.getContentPane().add(lblUsername);
        frame.getContentPane().add(usernameTextField);

        lblPcactivity = new JLabel("PcActivitySensorState:");
        lblPcactivity.setBounds(12, 12, 167, 15);
        frame.getContentPane().add(lblPcactivity);

        lblPcactivitystate = new JLabel("unavailable");
        lblPcactivitystate.setHorizontalAlignment(SwingConstants.RIGHT);
        lblPcactivitystate.setBounds(176, 12, 86, 15);
        frame.getContentPane().add(lblPcactivitystate);

        JSeparator separator = new JSeparator();
        separator.setBounds(12, 39, 250, 2);
        frame.getContentPane().add(separator);
    }
}
