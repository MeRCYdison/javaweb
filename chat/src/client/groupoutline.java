package client;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;

public class groupoutline extends JDialog {
    private String hostUserName;
    private List<JCheckBox> checkBoxes;
    private List<String> selectedUsers;

    public groupoutline(JFrame parent, List<String> userlist, String hostUserName) {
        super(parent, "群组窗口", true);
        setSize(400, 300);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        // 创建一个JPanel对象
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        // 创建复选框并添加到JPanel中
        checkBoxes = new ArrayList<>();
        JPanel checkBoxPanel = new JPanel();
        checkBoxPanel.setLayout(new BoxLayout(checkBoxPanel, BoxLayout.Y_AXIS));

        JCheckBox hostUserCheckBox = new JCheckBox(hostUserName);
        hostUserCheckBox.setSelected(true);
        hostUserCheckBox.setEnabled(false); // 设置hostUserName复选框为不可点击
        checkBoxes.add(hostUserCheckBox);
        checkBoxPanel.add(hostUserCheckBox);

        for (String user : userlist) {
            if (user.equals(hostUserName)) {
                continue; // 如果是 hostUserName，跳过
            }
            JCheckBox checkBox = new JCheckBox(user);
            checkBoxes.add(checkBox);
            checkBoxPanel.add(checkBox);
        }
        panel.add(new JScrollPane(checkBoxPanel), BorderLayout.CENTER);

        // 创建一个"确定"按钮
        JButton confirmButton = new JButton("确定");
        confirmButton.addActionListener(e -> {
            selectedUsers = new ArrayList<>();
            for (JCheckBox checkBox : checkBoxes) {
                if (checkBox.isSelected()) {
                    selectedUsers.add(checkBox.getText());
                }
            }
            this.setVisible(false);
        });
        // 创建一个新的JPanel，使用FlowLayout布局管理器，并将"确定"按钮添加到其中
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(confirmButton);

        // 将buttonPanel添加到panel的南部
        panel.add(buttonPanel, BorderLayout.SOUTH);

        // 将JPanel添加到JDialog中
        add(panel);

        setVisible(true);
    }
    public List<String> getSelectedUsers() {
        return selectedUsers;
    }
}