package server;

import javax.swing.*;
import java.awt.*;

public class serverlogin {
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
    private int port;
    private JTextField portField;

    public LoginOutline(String ip, int port) {

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


        // 添加端口号输入组件
        portField = new JTextField(String.valueOf(this.port), 20);
        this.add(createInputPanel("端口号:", portField), gbc);

        // 连接按钮
        JButton connectButton = new JButton("打开服务器");
        connectButton.setFont(new Font("SansSerif", Font.PLAIN, 24));
        gbc.insets = new Insets(20, 10, 10, 10);
        gbc.anchor = GridBagConstraints.CENTER;
        this.add(connectButton, gbc);

        // 连接按钮的点击事件
        connectButton.addActionListener(e -> {

            try {
                this.port = Integer.parseInt(portField.getText());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(null, "端口号必须是数字！");
                return;
            }

            if (this.port < 1024 || this.port > 65535) {
                JOptionPane.showMessageDialog(null, "端口号必须在1024到65535之间！");
                return;
            }

            try {
                serveroutline serveroutline = new serveroutline(this.port);
                this.setVisible(false);
                this.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "服务器无法创建！");
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
}
