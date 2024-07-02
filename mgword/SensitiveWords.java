import java.io.*;
import java.util.HashSet;
import java.util.Set;

public class SensitiveWords {
    private Set<String> words;

    public SensitiveWords() {
        words = new HashSet<>();
        loadWords();
    }

    private void loadWords() {
        try (BufferedReader br = new BufferedReader(new FileReader("sensitive_words.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                words.add(line.trim());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addWord(String word) {
        words.add(word);
        saveWords();
    }

    public void removeWord(String word) {
        words.remove(word);
        saveWords();
    }

    private void saveWords() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("sensitive_words.txt"))) {
            for (String word : words) {
                bw.write(word);
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Set<String> getWords() {
        return words;
    }
}
