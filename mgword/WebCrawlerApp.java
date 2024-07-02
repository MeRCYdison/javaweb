import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class WebCrawlerApp extends JFrame {
    private JTextField urlField;
    private JTextPane contentArea;
    private List<String> sensitiveWords;
    private WebCrawler webCrawler;

    public WebCrawlerApp() {

        sensitiveWords = new ArrayList<>();
        webCrawler = new WebCrawler(this,10);  // 使用 10 个线程进行爬取
        initComponents();
    }

    private void initComponents() {
        this.setTitle("Web Crawler");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(800, 600);
        this.setLayout(new BorderLayout());

        // URL 输入面板
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new FlowLayout());
        JLabel urlLabel = new JLabel("输入网址:");
        urlField = new JTextField(50);
        topPanel.add(urlLabel);
        topPanel.add(urlField);
        this.add(topPanel, BorderLayout.NORTH);

        // 内容显示面板
        contentArea = new JTextPane();
        contentArea.setContentType("text/html");
        JScrollPane scrollPane = new JScrollPane(contentArea);
        this.add(scrollPane, BorderLayout.CENTER);

        // 按钮面板
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new FlowLayout());

        JButton startButton = new JButton("开始爬取");
        JButton stopButton = new JButton("结束爬取");
        JButton showHtmlButton = new JButton("显示HTML内容");
        JButton showTextButton = new JButton("显示文本内容");
        JButton importButton = new JButton("导入敏感词库");
        JButton modifyButton = new JButton("修改敏感词库");

        // 添加按钮事件监听
        startButton.addActionListener(e -> startCrawling(urlField.getText()));
        stopButton.addActionListener(e -> stopCrawling());
        showHtmlButton.addActionListener(e -> showHtmlContent());
        showTextButton.addActionListener(e -> showTextContent());
        importButton.addActionListener(e -> importSensitiveWords());
        modifyButton.addActionListener(e -> modifySensitiveWords());

        bottomPanel.add(startButton);
        bottomPanel.add(stopButton);
        bottomPanel.add(showHtmlButton);
        bottomPanel.add(showTextButton);
        bottomPanel.add(importButton);
        bottomPanel.add(modifyButton);

        this.add(bottomPanel, BorderLayout.SOUTH);

        this.setVisible(true);
    }

    private void startCrawling(String url) {
        contentArea.setText("开始爬取: " + url + "<br>");
        webCrawler.startCrawling(url);
    }

    private void stopCrawling() {
        contentArea.setText(contentArea.getText() + "结束爬取<br>");
        webCrawler.stopCrawling();
    }

    private void showHtmlContent() {
        showUrlSelectionDialog("html");
    }

    private void showTextContent() {
        showUrlSelectionDialog("text");
    }

    private void showUrlSelectionDialog(String contentType) {
        Set<String> visitedUrls = webCrawler.getVisitedUrls();
        if (visitedUrls.isEmpty()) {
            JOptionPane.showMessageDialog(this, "没有已爬取的URL可供选择。", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog(this, "选择URL", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(400, 300);

        JPanel checkBoxPanel = new JPanel();
        checkBoxPanel.setLayout(new BoxLayout(checkBoxPanel, BoxLayout.Y_AXIS));

        List<JCheckBox> checkBoxes = new ArrayList<>();
        for (String url : visitedUrls) {
            JCheckBox checkBox = new JCheckBox(url);
            checkBoxes.add(checkBox);
            checkBoxPanel.add(checkBox);
        }

        JScrollPane scrollPane = new JScrollPane(checkBoxPanel);
        dialog.add(scrollPane, BorderLayout.CENTER);

        JButton showButton = new JButton("显示内容");
        showButton.addActionListener(e -> {
            contentArea.setText("");
            for (JCheckBox checkBox : checkBoxes) {
                if (checkBox.isSelected()) {
                    String url = checkBox.getText();
                    String content = contentType.equals("html") ? webCrawler.getHtmlContent(url) : webCrawler.getTextContent(url);
                    if (contentType.equals("text")) {
                        content = highlightSensitiveWords(content);
                    } else {
                        content = highlightSensitiveWordsInHtml(content);
                    }
                    contentArea.setText(contentArea.getText() + "URL: " + url + "<br>" + content + "<br><br>");
                }
            }
            dialog.dispose();
        });

        dialog.add(showButton, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private String highlightSensitiveWords(String content) {
        for (String word : sensitiveWords) {
            content = content.replaceAll("(?i)" + word, "<span style='background-color: yellow;'>" + word + "</span>");
        }
        return content;
    }

    private String highlightSensitiveWordsInHtml(String content) {
        for (String word : sensitiveWords) {
            content = content.replaceAll("(?i)" + word, "<span style='background-color: yellow;'>" + word + "</span>");
        }
        return content;
    }

    private void importSensitiveWords() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            loadSensitiveWordsFromFile(selectedFile);
        }
    }

    private void loadSensitiveWordsFromFile(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            sensitiveWords.clear();  // 清空已有的敏感词
            while ((line = reader.readLine()) != null) {
                sensitiveWords.add(line.trim());
                contentArea.setText("");
                contentArea.setText("敏感词库导入如下: " + sensitiveWords + "<br>");
            }
            System.out.println("敏感词库导入成功: " + sensitiveWords.size() + " 个敏感词");
        } catch (IOException e) {
            System.err.println("导入敏感词库失败: " + e.getMessage());
        }
    }

    private void modifySensitiveWords() {
        // 创建一个新的窗口用于修改敏感词
        JFrame modifyFrame = new JFrame("修改敏感词库");
        modifyFrame.setSize(400, 300);
        modifyFrame.setLayout(new BorderLayout());

        JTextArea wordsArea = new JTextArea();
        wordsArea.setLineWrap(true);
        wordsArea.setWrapStyleWord(true);

        // 加载当前的敏感词到文本区域
        for (String word : sensitiveWords) {
            wordsArea.append(word + "\n");
        }

        JScrollPane scrollPane = new JScrollPane(wordsArea);
        modifyFrame.add(scrollPane, BorderLayout.CENTER);

        // 保存按钮用于保存修改
        JButton saveButton = new JButton("保存");
        saveButton.addActionListener(e -> saveSensitiveWords(wordsArea.getText()));
        modifyFrame.add(saveButton, BorderLayout.SOUTH);

        modifyFrame.setVisible(true);
    }

    private void saveSensitiveWords(String wordsText) {
        sensitiveWords.clear();
        String[] wordsArray = wordsText.split("\\n");
        for (String word : wordsArray) {
            if (!word.trim().isEmpty()) {
                sensitiveWords.add(word.trim());
            }
        }
        System.out.println("敏感词库已更新: " + sensitiveWords.size() + " 个敏感词");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(WebCrawlerApp::new);
    }
}
