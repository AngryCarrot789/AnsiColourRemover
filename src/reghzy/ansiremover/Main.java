package reghzy.ansiremover;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.function.Consumer;

public class Main {
    public static JLabel STATUS_LABEL;

    public static void main(String[] args) {
        String rootDir = System.getProperty("user.dir");
        if (rootDir == null || rootDir.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Launch dir not supplied? (user.dir)");
            return;
        }

        File root = new File(rootDir);
        if (!root.isDirectory()) {
            JOptionPane.showMessageDialog(null, "Launch dir does not exist? " + rootDir);
            return;
        }

        JFrame frame = new JFrame("Ansi Colour Replacer");
        JList<String> list = new JList<>(new DefaultListModel<>());
        JScrollPane listBox = new JScrollPane(list);
        STATUS_LABEL = new JLabel("Processing files...");

        frame.add(listBox);
        frame.add(STATUS_LABEL, BorderLayout.SOUTH);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);
        frame.setVisible(true);

        Thread thread = new Thread(() -> {
            int count = scanFiles(root, list);
            SwingUtilities.invokeLater(() -> STATUS_LABEL.setText("Finished. Processed " + count + " .log files in total"));
        });
        thread.start();
    }

    public static int scanFiles(File folder, JList<String> list) {
        int count = 0;
        File[] folders = folder.listFiles();
        if (folders == null || folders.length == 0) {
            return count;
        }

        for (File file : folders) {
            if (file.isDirectory()) {
                count += scanFiles(file, list);
            }
            else if (file.isFile() && file.getName().endsWith(".log")) {
                try {
                    String content = readAllLines(file);
                    content = content.replaceAll("[\\u001b\\u009b]\\[[()#;?]*(?:[0-9]{1,4}(?:;[0-9]{0,4})*)?[0-9A-ORZcf-nqry=><]", "");
                    writeToFile(file, content);
                    SwingUtilities.invokeLater(() -> {
                        DefaultListModel<String> model = (DefaultListModel<String>) list.getModel();
                        model.addElement(file.getAbsolutePath());
                    });
                    count++;
                }
                catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }

        return count;
    }

    public static void writeToFile(File file, String text) throws IOException {
        try (FileWriter writer = new FileWriter(file, false)) {
            writer.write(text);
        }
        catch (FileNotFoundException e) {
            throw new IOException("Destination file is inaccessible or is a directory: " + file, e);
        }
    }

    public static void readLines(File file, Consumer<String> lineConsumer) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineConsumer.accept(line);
            }
        }
    }

    public static String readAllLines(File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        readLines(file, (a) -> sb.append(a).append('\n'));
        return sb.toString();
    }
}
