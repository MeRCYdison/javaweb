import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.uci.ics.jung.algorithms.layout.CircleLayout;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.renderers.Renderer;

public class WebCrawlerApp {
    private JFrame frame;
    private JTextField urlField;
    private JTextArea contentArea;
    private List<String> sensitiveWords;
    private WebCrawler webCrawler;

    private JButton startButton;
    private JButton stopButton;
    private JButton showHtmlButton;
    private JButton showTextButton;
    private JButton importButton;
    private JButton modifyButton;
    private JButton showmapButton;

    public WebCrawlerApp() {
        sensitiveWords = new ArrayList<>();
        webCrawler = new WebCrawler(500);  // 使用 10 个线程进行爬取
        webCrawler.setCrawlListener(this::onCrawling);
        initComponents();
    }

    private void initComponents() {
        frame = new JFrame("Web Crawler");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLayout(new BorderLayout());

        // URL 输入面板
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new FlowLayout());
        JLabel urlLabel = new JLabel("输入网址:");
        urlField = new JTextField(50);
        topPanel.add(urlLabel);
        topPanel.add(urlField);
        frame.add(topPanel, BorderLayout.NORTH);

        // 内容显示面板
        contentArea = new JTextArea();
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(contentArea);
        frame.add(scrollPane, BorderLayout.CENTER);

        // 按钮面板
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new FlowLayout());

        startButton = new JButton("开始爬取");
        stopButton = new JButton("结束爬取");
        showHtmlButton = new JButton("显示HTML内容");
        showTextButton = new JButton("显示文本内容");
        importButton = new JButton("导入敏感词库");
        modifyButton = new JButton("修改敏感词库");
        showmapButton = new JButton("可视化有向图网");

        // 初始化按钮状态
        stopButton.setEnabled(false);

        // 添加按钮事件监听
        startButton.addActionListener(e -> startCrawling(urlField.getText()));
        stopButton.addActionListener(e -> stopCrawling());
        showHtmlButton.addActionListener(e -> showHtmlContent());
        showTextButton.addActionListener(e -> showTextContent());
        importButton.addActionListener(e -> importSensitiveWords());
        modifyButton.addActionListener(e -> modifySensitiveWords());
        showmapButton.addActionListener(e -> showMap());

        bottomPanel.add(startButton);
        bottomPanel.add(stopButton);
        bottomPanel.add(showHtmlButton);
        bottomPanel.add(showTextButton);
        bottomPanel.add(importButton);
        bottomPanel.add(modifyButton);
        bottomPanel.add(showmapButton);

        frame.add(bottomPanel, BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    private void startCrawling(String url) {
        // 清空内容显示区域
        contentArea.setText("");

        contentArea.append("开始爬取: " + url + "\n");

        // 清空已爬取的网站列表，准备下一次扫描
        webCrawler = new WebCrawler(10);
        webCrawler.setCrawlListener(this::onCrawling);

        webCrawler.startCrawling(url);

        // 禁用除“结束爬取”按钮外的所有按钮
        startButton.setEnabled(false);
        showHtmlButton.setEnabled(false);
        showTextButton.setEnabled(false);
        importButton.setEnabled(false);
        modifyButton.setEnabled(false);
        showmapButton.setEnabled(false);
        stopButton.setEnabled(true);
    }

    private void stopCrawling() {
        contentArea.append("结束爬取\n");
        webCrawler.stopCrawling();

        // 启用所有按钮
        startButton.setEnabled(true);
        showHtmlButton.setEnabled(true);
        showTextButton.setEnabled(true);
        importButton.setEnabled(true);
        modifyButton.setEnabled(true);
        showmapButton.setEnabled(true);
        stopButton.setEnabled(false);
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
            JOptionPane.showMessageDialog(frame, "没有已爬取的URL可供选择。", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog(frame, "选择URL", true);
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
                    highlightSensitiveWords(content);
                }
            }
            dialog.dispose();
        });

        dialog.add(showButton, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void highlightSensitiveWords(String content) {
        contentArea.setText(content);
        Highlighter highlighter = contentArea.getHighlighter();
        Highlighter.HighlightPainter painter = new DefaultHighlighter.DefaultHighlightPainter(Color.YELLOW);
        //创建一个 HighlightPainter 对象，用于指定高亮显示的样式，这里选择黄色
        for (String word : sensitiveWords) {
            int index = content.indexOf(word);
            while (index >= 0) {
                try {
                    highlighter.addHighlight(index, index + word.length(), painter);
                    index = content.indexOf(word, index + word.length());
                } catch (BadLocationException e) {
                    e.printStackTrace();
                }
            }
        }
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
                contentArea.append("敏感词库导入如下: " + sensitiveWords + "\n");
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

    private void showMap() {
        // 创建一个有向图

        Graph<String, String> graph = new DirectedSparseMultigraph<>();
        Map<String, Set<String>> urlRelations = webCrawler.getUrlRelations();
        // 添加图的节点和边

        for (Map.Entry<String, Set<String>> entry : urlRelations.entrySet()) {
            String source = entry.getKey();
            Set<String> targets = entry.getValue();
            for (String target : targets) {
                graph.addEdge(source + " -> " + target, source, target);
            }
        }

        // 圆形布局
        CircleLayout<String, String> layout = new CircleLayout<>(graph);
        layout.setSize(new Dimension(500, 500));

        // 创建可视化组件
        VisualizationViewer<String, String> vv = new VisualizationViewer<>(layout);
        vv.setPreferredSize(new Dimension(600, 600));
        vv.getRenderContext().setVertexLabelTransformer(vertex -> vertex);
        vv.getRenderer().getVertexLabelRenderer().setPosition(Renderer.VertexLabel.Position.CNTR);

        // 添加鼠标交互
        DefaultModalGraphMouse<String, String> gm = new DefaultModalGraphMouse<>();
        vv.setGraphMouse(gm);

        // 显示窗口
        JFrame mapFrame = new JFrame("Graph Visualization");
        mapFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        mapFrame.getContentPane().add(vv);
        mapFrame.pack();
        mapFrame.setVisible(true);
    }

    private void onCrawling(String url) {
        SwingUtilities.invokeLater(() -> contentArea.append("正在爬取: " + url + "\n"));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(WebCrawlerApp::new);
    }
}
