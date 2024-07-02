import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.util.Set;

public class TextHighlighter {
    private Set<String> sensitiveWords;

    public TextHighlighter(Set<String> sensitiveWords) {
        this.sensitiveWords = sensitiveWords;
    }

    public void highlightSensitiveWords(String text, JTextPane textPane) {
        textPane.setText("");
        String[] words = text.split("\\s+");
        for (String word : words) {
            if (sensitiveWords.contains(word)) {
                appendToPane(textPane, word + " ", Color.RED);
            } else {
                appendToPane(textPane, word + " ", Color.BLACK);
            }
        }
    }

    private void appendToPane(JTextPane textPane, String msg, Color c) {
        StyledDocument doc = textPane.getStyledDocument();
        SimpleAttributeSet set = new SimpleAttributeSet();
        StyleConstants.setForeground(set, c);
        StyleConstants.setBold(set, true);
        try {
            doc.insertString(doc.getLength(), msg, set);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
}
