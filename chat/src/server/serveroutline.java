package server;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import org.json.JSONObject;

public class serveroutline extends JFrame {
    private static UserLabel selectedLabel = null; // 记录当前选中的标签
    private JTabbedPane tabbedPane1;
    private JPanel panel2;
    private JScrollPane scrollPane1;
    private JTextArea logArea;
    private JPanel southP;
    private JButton saveLogsButton;
    private JButton closeButton;
    private JPanel panel3;
    private JScrollPane scrollPane2;
    private JPanel userPanel;
    private JPanel southP2;
    private JButton kickButton;
    private JTextArea userInfoArea;
    private JPanel panel4;
    private JPanel southP3;
    private JButton broadcastButton;
    private JTextArea broadcastArea;
    private JScrollPane scrollBroadcastArea;
    private JSplitPane splitPane;
    private int port;
    private Set<String> users = new HashSet<>();
    private HashMap<String, Socket> userSockets = new HashMap<>();
    private Set<Socket> sockets = new HashSet<>();
    private ServerSocket serverSocket; // 新增成员变量
    private HashMap<String, ArrayList<String>> userMessages = new HashMap<>();

    public serveroutline(int port) {
        this.setTitle("服务器控制端");
        this.port = port;
        initComponents();
        addUser("公告", null); // 添加初始用户标签
        addUser("世界群聊", null);
        this.setVisible(true);
        new ServerSocketThread().start(); // 启动服务器监听线程
    }

    private void saveLogs(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Specify a file to save");

        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            try (PrintWriter out = new PrintWriter(new FileWriter(fileToSave))) {
                out.println(logArea.getText());
                JOptionPane.showMessageDialog(this, "Logs saved successfully to " + fileToSave.getAbsolutePath());
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error saving logs to " + fileToSave.getAbsolutePath(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void closeServer(ActionEvent e) {
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to close the server?", "Confirm Server Closure", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                // Close all client sockets
                for (Socket socket : sockets) {
                    socket.close();
                }
                sockets.clear();
                logArea.append("Server closed.\n");

                // Close the server socket
                if (serverSocket != null) {
                    serverSocket.close();
                    serverSocket = null;
                }
                // 关闭服务器后，提示是退出还是重启
                int choice = JOptionPane.showConfirmDialog(this, "Do you want to restart the server?", "Server Closed", JOptionPane.YES_NO_OPTION);
                if (choice == JOptionPane.YES_OPTION) {
                    serverlogin serverlogin = new serverlogin(); // 重新启动服务器
                    this.dispose(); // 关闭当前窗口
                } else {
                    System.exit(0); // 退出程序
                }

            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error closing the server: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void kickUser(ActionEvent e) {
        if (selectedLabel == null) {
            JOptionPane.showMessageDialog(this, "No user selected.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String userToKick = selectedLabel.getUserName();
        Socket socketToClose = userSockets.get(userToKick);

        // Send kick message to the user
        JSONObject kickMessage = new JSONObject();
        kickMessage.put("type", "KICK");
        kickMessage.put("content", "You have been kicked from the server.");
        try {
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(socketToClose.getOutputStream()));
            pw.println(kickMessage.toString());
            pw.flush();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        // Close the socket
        try {
            socketToClose.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        // Remove the user from the user list and update the user list
        removeUser(userToKick);
        broadcastUserList();
    }

    private void broadcastMessage(ActionEvent e) {
        String messageContent = broadcastArea.getText().trim();
        if (messageContent.isEmpty()) {
            return;
        }

        JSONObject message = new JSONObject();
        message.put("type", "ANNOUNCEMENT");
        message.put("content", "公告: " + messageContent);

        for (Socket temp : sockets) {
            try {
                PrintWriter pw = new PrintWriter(new OutputStreamWriter(temp.getOutputStream()));
                pw.println(message.toString());
                pw.flush();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        logArea.append("公告已发送: " + messageContent + "\n");
        broadcastArea.setText("");
    }

    private void initComponents() {
        tabbedPane1 = new JTabbedPane();
        panel2 = new JPanel();
        scrollPane1 = new JScrollPane();
        logArea = new JTextArea();
        southP = new JPanel();
        saveLogsButton = new JButton();
        closeButton = new JButton();
        panel3 = new JPanel();
        scrollPane2 = new JScrollPane();
        userPanel = new JPanel();
        southP2 = new JPanel();
        kickButton = new JButton();
        userInfoArea = new JTextArea();
        panel4 = new JPanel();
        southP3 = new JPanel();
        broadcastButton = new JButton();
        broadcastArea = new JTextArea();
        scrollBroadcastArea = new JScrollPane(broadcastArea);
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

        setPreferredSize(new Dimension(800, 500));
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        tabbedPane1.setBackground(new Color(0x009999));

        // Server Control Panel
        panel2.setLayout(new BorderLayout());
        logArea.setEditable(false);
        logArea.setBorder(new TitledBorder("Logs"));
        scrollPane1.setViewportView(logArea);
        panel2.add(scrollPane1, BorderLayout.CENTER);

        southP.setLayout(new GridLayout(1, 3));
        saveLogsButton.setText("保存 Logs");
        saveLogsButton.addActionListener(e -> saveLogs(e));
        southP.add(saveLogsButton);
        closeButton.setText("关闭服务器");
        closeButton.setBackground(new Color(0xff0033));
        closeButton.addActionListener(e -> closeServer(e));
        southP.add(closeButton);

        panel2.add(southP, BorderLayout.SOUTH);

        tabbedPane1.addTab("服务器控制", panel2);

        // User Control Panel
        panel3.setLayout(new BorderLayout());

        // Set layout and border for userPanel
        userPanel.setLayout(new BoxLayout(userPanel, BoxLayout.Y_AXIS));
        userPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        scrollPane2.setViewportView(userPanel);
        splitPane.setLeftComponent(scrollPane2);

        userInfoArea.setEditable(false);
        userInfoArea.setBorder(new TitledBorder("User Info"));
        splitPane.setRightComponent(userInfoArea);

        panel3.add(splitPane, BorderLayout.CENTER);

        southP2.setLayout(new GridLayout(1, 2));

        kickButton.setText("临时踢出");
        kickButton.addActionListener(e -> kickUser(e));
        southP2.add(kickButton);
        panel3.add(southP2, BorderLayout.SOUTH);

        tabbedPane1.addTab("用户控制", panel3);

        // Broadcast Panel
        panel4.setLayout(new BorderLayout());
        broadcastArea.setLineWrap(true);
        broadcastArea.setWrapStyleWord(true);
        broadcastArea.setBorder(new TitledBorder("Broadcast Message"));
        broadcastArea.setFont(new Font("Serif", Font.PLAIN, 20)); // 设置字体大小为20
        scrollBroadcastArea.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollBroadcastArea.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        panel4.add(scrollBroadcastArea, BorderLayout.CENTER);

        southP3.setLayout(new GridLayout(1, 1));
        broadcastButton.setText("发布公告");
        broadcastButton.addActionListener(e -> broadcastMessage(e));
        southP3.add(broadcastButton);
        panel4.add(southP3, BorderLayout.SOUTH);

        tabbedPane1.addTab("发布公告", panel4);

        contentPane.add(tabbedPane1, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

    public void addUser(String user, Socket socket) {
        if (users.add(user)) { // 只有在用户不重复时才添加
            UserLabel userLabel = new UserLabel(user);
            if ("公告".equals(user)) {
                userPanel.add(userLabel, 0); // 将“公告”添加到最顶部
            } else if ("世界群聊".equals(user)) {
                // 如果已经存在“公告”，将“世界群聊”添加到第二个位置，否则添加到顶部

                userPanel.add(userLabel, 1);
            } else {
                userPanel.add(userLabel); // 其他用户添加到末尾
            }
            userPanel.revalidate();
            userPanel.repaint();
            if (socket != null) {
                userSockets.put(user, socket); // 添加用户与套接字的映射
            }
        }
    }



    public void removeUser(String user) {
        if (users.remove(user)) {
            userSockets.remove(user); // 移除用户与套接字的映射
            for (Component component : userPanel.getComponents()) {
                if (component instanceof UserLabel && ((UserLabel) component).getUserName().equals(user)) {
                    userPanel.remove(component);
                    userPanel.revalidate();
                    userPanel.repaint();
                    break;
                }
            }
        }
    }

    public void displayUserMessages(String user) {
        ArrayList<String> messages = userMessages.get(user);
        userInfoArea.setText(""); // 清空userInfoArea
        if (messages != null) {
            for (String message : messages) {
                userInfoArea.append(message + "\n"); // 将消息添加到userInfoArea
            }
        }
    }

    class UserLabel extends JPanel {
        private JLabel label;
        private String userName;

        public UserLabel(String user) {
            this.userName = user;
            setLayout(new BorderLayout());
            label = new JLabel(user);
            label.setFont(new Font("Serif", Font.PLAIN, 30));
            label.setOpaque(true);
            label.setBackground(Color.WHITE);
            label.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            add(label, BorderLayout.CENTER);
            label.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            // 设置固定的高度
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            this.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (selectedLabel != null) {
                        selectedLabel.label.setBackground(Color.WHITE); // 取消当前选中的标签
                    }
                    selectedLabel = UserLabel.this; // 更新为新选中的标签
                    label.setBackground(Color.LIGHT_GRAY); // 设置新选中的标签背景颜色
                    displayUserMessages(userName); // 显示用户的消息
                }
            });
        }

        public String getUserName() {
            return userName;
        }
    }

    class ServerSocketThread extends Thread {
        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(port); // 初始化 serverSocket
                logArea.append("Server started on port: " + port + "\n");
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    sockets.add(clientSocket);
                    logArea.append("New connection from: " + clientSocket.getInetAddress() + "\n");
                    new Thread(new ServerThread(clientSocket)).start();
                }
            } catch (IOException e) {
                logArea.append("Error in server socket: " + e.getMessage() + "\n");
            }
        }
    }

    class ServerThread implements Runnable {
        private Socket socket;
        private String userName;

        public ServerThread(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter pw;
                String message;

                while ((message = br.readLine()) != null) {
                    JSONObject json = new JSONObject(message);
                    String type = json.getString("type");
                    String content = json.getString("content");

                    switch (type) {
                        case "JOIN":
                            this.userName = content;
                            addUser(content, socket);
                            broadcastUserList();
                            logArea.append(" [ " + content + " 上线了 ]\n");
                            break;
                        case "MESSAGE":
                            handleMessage(json);
                            break;
                        case "USER_LIST_REQUEST":
                            sendUserList();
                            break;
                    }
                }
            } catch (IOException e) {
                if (this.userName != null) {
                    removeUser(this.userName);
                    broadcastUserList();
                    logArea.append(" [ " + this.userName + " 断开连接 ]\n");
                }
                sockets.remove(socket);
            }
        }

        private void handleMessage(JSONObject message) {
            String content = message.getString("content");
            String from = message.has("from") ? message.getString("from") : "[世界群聊]";
            logArea.append(content + "\n");

            // 将消息添加到用户的消息列表中
            ArrayList<String> senderMessages = userMessages.getOrDefault(userName, new ArrayList<>());
            senderMessages.add(userName + ": " + content);
            userMessages.put(userName, senderMessages);

            // 如果消息是私人消息，将其添加到接收者的消息列表中
            if (message.has("to")) {
                String recipient = message.getString("to");
                ArrayList<String> recipientMessages = userMessages.getOrDefault(recipient, new ArrayList<>());
                recipientMessages.add(recipient + " (private from " + userName + "): " + content);
                userMessages.put(recipient, recipientMessages);
            } else {
                // 将公共消息添加到“世界群聊”的消息列表中
                ArrayList<String> worldMessages = userMessages.getOrDefault("世界群聊", new ArrayList<>());
                worldMessages.add("[世界群聊] " + from + ": " + content);
                userMessages.put("世界群聊", worldMessages);
            }

            // 如果当前选中的用户是消息的发送者或接收者，更新其消息记录
            if (selectedLabel != null && (selectedLabel.getUserName().equals(userName) || selectedLabel.getUserName().equals(message.getString("to")))) {
                displayUserMessages(selectedLabel.getUserName());
            }

            if (message.has("to")) { // 处理私人消息
                String recipient = message.getString("to");
                Socket recipientSocket = userSockets.get(recipient);
                if (recipientSocket != null) {
                    try {
                        message.put("from", userName);
                        PrintWriter pw = new PrintWriter(new OutputStreamWriter(recipientSocket.getOutputStream()));
                        pw.println(message.toString());
                        pw.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else { // 处理公共消息
                for (Socket temp : sockets) {
                    try {
                        PrintWriter pw = new PrintWriter(new OutputStreamWriter(temp.getOutputStream()));
                        pw.println(message.toString());
                        pw.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            if ("KICK".equals(message.getString("type"))) {
                // 处理踢出消息
                handleKick(message);
            }
        }


        private void handleKick(JSONObject message) {
            // Send kick message to the user
            JSONObject kickMessage = new JSONObject();
            kickMessage.put("type", "KICK");
            kickMessage.put("content", "You have been kicked from the server.");
            try {
                PrintWriter pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
                pw.println(kickMessage.toString());
                pw.flush();
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            // Close the socket
            try {
                socket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            // Remove the user from the user list and update the user list
            removeUser(userName);
            broadcastUserList();
        }

        private void sendUserList() {
            JSONObject userListMessage = new JSONObject();
            userListMessage.put("type", "USER_LIST");
            userListMessage.put("content", users);

            try {
                PrintWriter pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
                pw.println(userListMessage.toString());
                pw.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void broadcastUserList() {
        JSONObject userListMessage = new JSONObject();
        userListMessage.put("type", "USER_LIST");
        userListMessage.put("content", users);

        for (Socket temp : sockets) {
            try {
                PrintWriter pw = new PrintWriter(new OutputStreamWriter(temp.getOutputStream()));
                pw.println(userListMessage.toString());
                pw.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
