package client;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.util.*;
import org.json.JSONArray;
import org.json.JSONObject;

public class clientoutline extends JFrame {
    private static String IP;
    private static int PORT;
    private static String NAME;
    private static UserLabel selectedLabel = null; // 记录当前选中的标签

    JPanel outline = new JPanel(); // 容纳聊天内容、广播
    JTextArea communication = new JTextArea(10, 20);
    JScrollPane scrollcommunication = new JScrollPane(communication);
    JTextArea textArea = new JTextArea(); // 使用JTextArea代替JTextField
    JScrollPane scrollPane = new JScrollPane(textArea);
    JPanel choose = new JPanel();
    JLabel charCountLabel = new JLabel("0/200");
    private JButton jbt = new JButton("发送");
    JPanel totaluserpanel = new JPanel(new BorderLayout());
    JPanel userPanel = new JPanel();
    JScrollPane ScrolluserPanel = new JScrollPane(userPanel); // 容纳用户名列表
    JButton groupcreate = new JButton("创建群组");
    JPanel groupcreatebuttonPanel = new JPanel(new BorderLayout());
    JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    private UserLabel defaultLabel = new UserLabel("世界群聊");

    Set<String> users = new HashSet<>(); // 用于存储用户名
    private HashMap<String, ArrayList<String>> chatHistory = new HashMap<>(); // 用于存储聊天记录

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;

    public clientoutline(String ip, int port, String name) throws HeadlessException, IOException {
        IP = ip;
        PORT = port;
        NAME = name;

        connectToServer();

        communication.setEditable(false);
        communication.setFont(new Font("Serif", Font.PLAIN, 30));
        communication.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        communication.setLineWrap(true);
        communication.setWrapStyleWord(true);
        communication.append(NAME + ", 欢迎来到聊天室\n");

        outline.setLayout(new BorderLayout());
        outline.add(scrollcommunication, BorderLayout.CENTER);
        outline.add(choose, BorderLayout.SOUTH);

        groupcreate.setFont(new Font("Serif", Font.PLAIN, 20));
        groupcreatebuttonPanel.add(groupcreate, BorderLayout.SOUTH);

        ScrolluserPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        ScrolluserPanel.setPreferredSize(new Dimension(200, ScrolluserPanel.getPreferredSize().height));
        userPanel.setLayout(new BoxLayout(userPanel, BoxLayout.Y_AXIS)); // 使用垂直布局

        totaluserpanel.add(ScrolluserPanel, BorderLayout.CENTER);
        totaluserpanel.add(groupcreatebuttonPanel, BorderLayout.SOUTH);

        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setFont(new Font("Serif", Font.PLAIN, 30));
        textArea.setEditable(true);

        charCountLabel.setFont(new Font("Serif", Font.PLAIN, 20));

        jbt.setFont(new Font("Serif", Font.PLAIN, 20));

        choose.setPreferredSize(new Dimension(500, 200));
        choose.setLayout(new BorderLayout());
        choose.add(scrollPane, BorderLayout.CENTER);
        choose.add(jbt, BorderLayout.EAST);
        choose.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        choose.add(charCountLabel, BorderLayout.SOUTH);
        addUser(NAME);

        this.setTitle(NAME + "的聊天室");
        this.setLayout(new BorderLayout());
        this.add(splitPane, BorderLayout.CENTER);
        splitPane.setLeftComponent(totaluserpanel);
        splitPane.setRightComponent(outline);
        splitPane.setDividerLocation(200); // 设置分隔条初始位置

        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.setLocation(0, 0);
        this.setSize(1000, 800);
        this.setVisible(true);
        this.setMinimumSize(new Dimension(800, 500)); // 设置最小高度为500
        selectedLabel = defaultLabel;
        removeInputComponents();
        textArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateCharCount();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateCharCount();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateCharCount();
            }

            private void updateCharCount() {
                int charCount = textArea.getText().length();
                charCountLabel.setText(charCount + "/200");
                if (charCount >= 200) {
                    textArea.addKeyListener(new KeyAdapter() {
                        public void keyTyped(KeyEvent e) {
                            if (charCount >= 200 && !Character.isISOControl(e.getKeyChar())) {
                                e.consume();
                            }
                        }
                    });
                } else {
                    for (KeyListener kl : textArea.getKeyListeners()) {
                        textArea.removeKeyListener(kl);
                    }
                }
            }
        });

        groupcreate.addActionListener(e -> {
//            groupoutline groupOutline = new groupoutline(this);
//            groupOutline.setLocationRelativeTo(null);
//            groupOutline.addWindowListener(new WindowAdapter() {
//            });
        });

        jbt.addActionListener(e -> sendMessage());
    }

    private void connectToServer() throws IOException {
        socket = new Socket(IP, PORT);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

        // 发送JOIN消息
        JSONObject joinMessage = new JSONObject();
        joinMessage.put("type", "JOIN");
        joinMessage.put("content", NAME);
        writer.println(joinMessage.toString());

        // 开始监听服务器消息
        new Thread(() -> {
            try {
                String message;
                while ((message = reader.readLine()) != null) {
                    JSONObject json = new JSONObject(message);
                    String type = json.getString("type");

                    switch (type) {
                        case "USER_LIST":
                            updateUsersList(json.getJSONArray("content"));
                            break;
                        case "MESSAGE":
                            handleMessage(json);
                            break;
                        case "ANNOUNCEMENT":
                            handleAnnouncement(json);
                            break;
                        case "KICK":
                            handleKick(json); // 处理KICK消息
                            break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void handleMessage(JSONObject message) {
        String content = message.getString("content");
        String from = message.has("from") ? message.getString("from") : "[世界群聊]";
        String recipient = message.has("to") ? message.getString("to") : "[世界群聊]";

        if (recipient.equals("[世界群聊]")) {
            communication.append("[世界群聊] " + from + ": " + content + "\n");
            saveChatHistory("世界群聊", from + ": " + content);
        } else if (recipient.equals(NAME)) {
            communication.append("[私信] " + from + ": " + content + "\n");
            saveChatHistory(from, content);
        } else {
            communication.append(from + " (private): " + content + "\n");
            saveChatHistory(from, content);
        }
    }



    private void handleAnnouncement(JSONObject message) {
        String content = message.getString("content");
        communication.append("[公告] " + content + "\n");
        saveChatHistory("公告", content);
    }

    private void handleKick(JSONObject message) {
        JOptionPane.showMessageDialog(this, "你已被踢出聊天室", "警告", JOptionPane.WARNING_MESSAGE);
        int result = JOptionPane.showConfirmDialog(this, "你是否要退出?", "确认", JOptionPane.YES_NO_OPTION);
        if (result == JOptionPane.YES_OPTION) {
            System.exit(0);
        }
    }

    private void updateUsersList(JSONArray userList) {
        userPanel.removeAll();
        users.clear();
        if (userList.toList().contains("公告")) {
            addUser("公告");
        }

        // Add "世界群聊" next if it is in the list
        if (userList.toList().contains("世界群聊")) {
            addUser("世界群聊");
        }
        for (int i = 0; i < userList.length(); i++) {
            String user = userList.getString(i);
            if (!user.equals("公告") && !user.equals("世界群聊")) {
                addUser(user);
            }
        }
    }

    private void sendMessage() {
        String messageContent = textArea.getText();
        if (messageContent.isEmpty()) {
            return;
        }

        JSONObject message = new JSONObject();
        if (selectedLabel == null) {
            selectedLabel = defaultLabel;
        }
        if (!selectedLabel.getUserName().equals("服务器") && !selectedLabel.getUserName().equals(NAME) && !selectedLabel.getUserName().equals("世界群聊")) {
            message.put("to", selectedLabel.getUserName());
            message.put("content", NAME + " (private): " + messageContent);
            message.put("type", "MESSAGE");
            // 发送私信消息给选中的用户
            writer.println(message.toString());
            saveChatHistory(selectedLabel.getUserName(), NAME + " (private): " + messageContent);
            communication.append("[私信] " + NAME + ": " + messageContent + "\n");
        }
        else if (selectedLabel.getUserName().equals(NAME)) {
            communication.append("[自发] " + NAME + ": " + messageContent + "\n");
            saveChatHistory(NAME, "[自发] " + NAME + ": " + messageContent);
        } else {
            message.put("content", NAME + ": " + messageContent);
            message.put("type", "MESSAGE");
            writer.println(message.toString());
            saveChatHistory("世界群聊ChatHistory", NAME + ": " + messageContent);
        }

        textArea.setText("");
    }


    private void saveChatHistory(String user, String message) {
        String key;
        switch (user) {
            case "世界群聊":
                key = "wrld";
                break;
            case "公告":
                key = "announcement";
                break;
            default:
                key = user;
                break;
        }

        if (!chatHistory.containsKey(key)) {
            chatHistory.put(key, new ArrayList<>());
        }
        chatHistory.get(key).add(message);
    }

    private void loadChatHistory(String user) {
        String key;
        switch (user) {
            case "世界群聊":
                key = "wrld";
                break;
            case "公告":
                key = "announcement";
                break;
            default:
                key = user;
                break;
        }

        communication.setText("");
        ArrayList<String> history = chatHistory.get(key);
        if (history != null) {
            for (String message : history) {
                communication.append(message + "\n");
            }
        }
    }

    public void addUser(String user) {
        if (!users.contains(user)) {
            users.add(user);
            UserLabel userLabel = new UserLabel(user);
            userPanel.add(userLabel);
            userPanel.revalidate();
            userPanel.repaint();
        }
    }

    public void removeUser(String user) {
        users.remove(user);
        for (Component component : userPanel.getComponents()) {
            if (component instanceof UserLabel && ((UserLabel) component).getUserName().equals(user)) {
                userPanel.remove(component);
                userPanel.revalidate();
                userPanel.repaint();
                break;
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
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

            this.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (selectedLabel != null) {
                        selectedLabel.label.setBackground(Color.WHITE);
                    }
                    selectedLabel = UserLabel.this;
                    label.setBackground(Color.LIGHT_GRAY);

                    loadChatHistory(userName);

                    if ("公告".equals(userName)) {
                        textArea.setEditable(false);
                        removeInputComponents();
                    } else {
                        textArea.setEditable(true);
                        addInputComponents();
                    }
                }
            });
        }

        public String getUserName() {
            return userName;
        }
    }

    private void removeInputComponents() {
        choose.remove(scrollPane);
        choose.remove(jbt);
        choose.remove(charCountLabel);
        choose.revalidate();
        choose.repaint();
    }

    private void addInputComponents() {
        choose.add(scrollPane, BorderLayout.CENTER);
        choose.add(jbt, BorderLayout.EAST);
        choose.add(charCountLabel, BorderLayout.SOUTH);
        choose.revalidate();
        choose.repaint();
    }
}
