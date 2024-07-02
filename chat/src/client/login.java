package client;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import org.json.JSONObject;
import java.util.HashSet;
import org.json.JSONArray;
public class login {
    private static String defaultIP = "127.0.0.1";
    private static int defaultPort = 54321;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LoginOutline loginOutline = new LoginOutline(defaultIP, defaultPort);
        });
    }
}

class LoginOutline extends JFrame {
    private String username;
    private String ip;
    private int port;
    private JTextField usernameField;
    private JTextField ipField;
    private JTextField portField;

    public LoginOutline(String ip, int port) {
        this.ip = ip;
        this.port = port;

        this.setTitle("登录");
        this.setSize(800, 600);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);
        this.setResizable(true);
        this.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor = GridBagConstraints.NORTH;

        // 添加用户名输入组件
        usernameField = new JTextField(20);
        this.add(createInputPanel("用户名:", usernameField), gbc);

        // 添加IP地址输入组件
        ipField = new JTextField(this.ip, 20);
        this.add(createInputPanel("IP地址:", ipField), gbc);

        // 添加端口号输入组件
        portField = new JTextField(String.valueOf(this.port), 20);
        this.add(createInputPanel("端口号:", portField), gbc);

        // 连接按钮
        JButton connectButton = new JButton("连接并登录");
        connectButton.setFont(new Font("SansSerif", Font.PLAIN, 24));
        gbc.insets = new Insets(20, 10, 10, 10);
        gbc.anchor = GridBagConstraints.CENTER;
        this.add(connectButton, gbc);

        // 连接按钮的点击事件
        connectButton.addActionListener(e -> {
            this.username = usernameField.getText();
            this.ip = ipField.getText();
            try {
                this.port = Integer.parseInt(portField.getText());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(null, "端口号必须是数字！");
                return;
            }
            if (usernameField.getText().isEmpty() || ipField.getText().isEmpty() || portField.getText().isEmpty()) {
                JOptionPane.showMessageDialog(null, "请输入用户名、IP和端口！");
                return;
            }
            if (this.port < 1 || this.port > 65535) {
                JOptionPane.showMessageDialog(null, "端口号必须在1到65535之间！");
                return;
            }
            try {
                if (isUsernameTaken(this.username)) {
                    JOptionPane.showMessageDialog(null, "用户名已存在，请选择其他用户名！");
                    return;
                }
                if (this.username.equals("世界群聊")|| this.username.equals("服务器")) {
                    JOptionPane.showMessageDialog(null, "不允许此类用户名");
                    return;
                }
                clientoutline clientoutline = new clientoutline(this.ip, this.port, this.username);
                this.setVisible(false);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "无法连接至服务器！");
                return;
            }
        });

        this.setVisible(true);
    }

    private JPanel createInputPanel(String labelText, JTextField textField) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        JLabel label = new JLabel(labelText);
        Dimension labelSize = new Dimension(100, 50); // 设置你希望的宽度和高度
        label.setPreferredSize(labelSize);
        label.setFont(new Font("SansSerif", Font.BOLD, 24)); // 设置标签为粗体和更大的字体
        gbc.insets = new Insets(10, 10, 5, 20);
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(label, gbc);

        textField.setFont(new Font("SansSerif", Font.PLAIN, 24)); // 设置输入框的字体为24
        gbc.gridx = 1;
        gbc.insets = new Insets(5, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.EAST; // 修改这里，使得输入框左对齐
        panel.add(textField, gbc);

        return panel;
    }

    private boolean isUsernameTaken(String username) {
        try (Socket socket = new Socket(this.ip, this.port);
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // Send request for user list
            JSONObject request = new JSONObject();
            request.put("type", "USER_LIST_REQUEST");
            request.put("content", "");
            out.println(request.toString());

            // Read response
            String response = in.readLine();
            JSONObject jsonResponse = new JSONObject(response);
            if ("USER_LIST".equals(jsonResponse.getString("type"))) {
                JSONArray contentArray = jsonResponse.getJSONArray("content");
                HashSet<String> users = new HashSet<>();
                for (int i = 0; i < contentArray.length(); i++) {
                    users.add(contentArray.getString(i));
                }
                return users.contains(username);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

}
