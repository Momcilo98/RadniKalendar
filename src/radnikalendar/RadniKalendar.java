
package radnikalendar;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class RadniKalendar extends JFrame {

    private JTextField[] dayFields;
    private int[] opcijeVrednosti = {2380, 3000, 3500, 0, 1428, 2380};
    private final int putniTroskovi = 320;
    private final int topliObrok = 250;
    private final Map<String, Map<Integer, String>> zarade = new HashMap<>();
    private final String DATA_FILE = "zarade.txt";
    private final String SETTINGS_FILE = "settings.txt";
    private JComboBox<String> monthComboBox;
    private JComboBox<Integer> yearComboBox;
    private JPanel calendarPanel;
    private int currentYear;
    private int currentMonth;

    public RadniKalendar() {
        setTitle("Radni Kalendar");
        setSize(800, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        loadSettings();
        loadZarade();

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new FlowLayout());

        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMMM");
        monthComboBox = new JComboBox<>();
        for (int month = 1; month <= 12; month++) {
            monthComboBox.addItem(YearMonth.of(2000, month).format(monthFormatter));
        }
        monthComboBox.addActionListener(e -> updateCalendar());
        controlPanel.add(monthComboBox);

        yearComboBox = new JComboBox<>();
        for (int year = 2024; year <= 2034; year++) {
            yearComboBox.addItem(year);
        }
        yearComboBox.addActionListener(e -> updateCalendar());
        controlPanel.add(yearComboBox);

        add(controlPanel, BorderLayout.NORTH);

        calendarPanel = new JPanel();
        calendarPanel.setLayout(new GridLayout(0, 7));
        add(calendarPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(1, 5));

        JButton settingsButton = new JButton("Podesavanje vrednosti");
        settingsButton.addActionListener(e -> showSettingsDialog());

        JButton legendButton = new JButton("Legenda");
        JPopupMenu legendMenu = new JPopupMenu();

        JMenuItem opcija1 = new JMenuItem("Opcija 1: regularna zarada pon-pet");
        opcija1.addActionListener(e -> insertOption(1));
        legendMenu.add(opcija1);

        JMenuItem opcija2 = new JMenuItem("Opcija 2: prekovremeno Subota");
        opcija2.addActionListener(e -> insertOption(2));
        legendMenu.add(opcija2);

        JMenuItem opcija3 = new JMenuItem("Opcija 3: prekovremeno Nedelja");
        opcija3.addActionListener(e -> insertOption(3));
        legendMenu.add(opcija3);

        JMenuItem opcija4 = new JMenuItem("Opcija 4: neplacen neradan dan");
        opcija4.addActionListener(e -> insertOption(4));
        legendMenu.add(opcija4);

        JMenuItem opcija5 = new JMenuItem("Opcija 5: bolovanje");
        opcija5.addActionListener(e -> insertOption(5));
        legendMenu.add(opcija5);

        JMenuItem opcija6 = new JMenuItem("Opcija 6: praznik/godisnji odmor");
        opcija6.addActionListener(e -> insertOption(6));
        legendMenu.add(opcija6);

        legendButton.addActionListener(e -> legendMenu.show(legendButton, legendButton.getWidth() / 2, -legendMenu.getPreferredSize().height));

        JButton sumButton = new JButton("Obračunaj Mesec");
        sumButton.addActionListener(e -> calculateSum());

        JButton yearlySumButton = new JButton("Obračunaj Godinu");
        yearlySumButton.addActionListener(e -> calculateYearlySum());

        JButton resetButton = new JButton("Reset");
        resetButton.addActionListener(e -> resetEntries());

        buttonPanel.add(settingsButton);
        buttonPanel.add(legendButton);
        buttonPanel.add(sumButton);
        buttonPanel.add(yearlySumButton);
        buttonPanel.add(resetButton);

        JPanel mainButtonPanel = new JPanel(new BorderLayout());
        mainButtonPanel.add(buttonPanel, BorderLayout.CENTER);
        add(mainButtonPanel, BorderLayout.SOUTH);

        LocalDate now = LocalDate.now();
        currentYear = now.getYear();
        currentMonth = now.getMonthValue();
        monthComboBox.setSelectedIndex(currentMonth - 1);
        yearComboBox.setSelectedItem(currentYear);
        updateCalendar();
    }

    private void updateCalendar() {
        if (dayFields != null) {
            saveCurrentEntries(); // Save current entries before updating the calendar
        }

        currentMonth = monthComboBox.getSelectedIndex() + 1;
        currentYear = (int) yearComboBox.getSelectedItem();
        LocalDate firstDayOfMonth = LocalDate.of(currentYear, currentMonth, 1);
        int daysInMonth = firstDayOfMonth.lengthOfMonth();
        int dayOfWeek = firstDayOfMonth.getDayOfWeek().getValue() % 7;

        calendarPanel.removeAll();
        calendarPanel.setLayout(new GridLayout(0, 7));

        String[] days = {"Ned", "Pon", "Uto", "Sre", "Čet", "Pet", "Sub"};
        for (String day : days) {
            calendarPanel.add(new JLabel(day, SwingConstants.CENTER));
        }

        for (int i = 0; i < dayOfWeek; i++) {
            calendarPanel.add(new JLabel(""));
        }

        dayFields = new JTextField[daysInMonth];
        for (int day = 1; day <= daysInMonth; day++) {
            JTextField dayField = new JTextField();
            dayField.setToolTipText("Enter data for day " + day);
            String key = getCurrentKey();
            if (zarade.containsKey(key) && zarade.get(key).containsKey(day)) {
                dayField.setText(zarade.get(key).get(day));
            }
            dayField.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    saveCurrentEntries();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    saveCurrentEntries();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    saveCurrentEntries();
                }
            });
            calendarPanel.add(dayField);
            dayFields[day - 1] = dayField;
        }

        calendarPanel.revalidate();
        calendarPanel.repaint();
    }

    private String getCurrentKey() {
        return currentYear + "-" + currentMonth;
    }

    private void showSettingsDialog() {
        JDialog settingsDialog = new JDialog(this, "Podesavanje vrednosti", true);
        settingsDialog.setLayout(new GridLayout(9, 2));

        JTextField[] optionFields = new JTextField[6];
        for (int i = 0; i < 6; i++) {
            settingsDialog.add(new JLabel("Opcija " + (i + 1) + " vrednost:"));
            optionFields[i] = new JTextField(String.valueOf(opcijeVrednosti[i]));
            settingsDialog.add(optionFields[i]);
        }

        JTextField putniTroskoviField = new JTextField(String.valueOf(putniTroskovi));
        JTextField topliObrokField = new JTextField(String.valueOf(topliObrok));
        putniTroskoviField.setEditable(false);
        topliObrokField.setEditable(false);

        settingsDialog.add(new JLabel("Putni troskovi:"));
        settingsDialog.add(putniTroskoviField);
        settingsDialog.add(new JLabel("Topli obrok:"));
        settingsDialog.add(topliObrokField);

        JButton saveButton = new JButton("Sacuvaj");
        saveButton.addActionListener(e -> {
            for (int i = 0; i < 6; i++) {
                opcijeVrednosti[i] = Integer.parseInt(optionFields[i].getText());
            }
            saveSettings();
            settingsDialog.dispose();
        });

        settingsDialog.add(saveButton);
        settingsDialog.pack();
        settingsDialog.setLocationRelativeTo(this);
        settingsDialog.setVisible(true);
    }

    private void insertOption(int option) {
        for (JTextField dayField : dayFields) {
            if (dayField.getText().isEmpty()) {
                dayField.setText(String.valueOf(option));
                saveCurrentEntries();
                break;
            }
        }
    }

    private void calculateSum() {
        int totalSum = 0;
        int zarada = 0;
        int ukupniPutniTroskovi = 0;
        int ukupniTopliObrok = 0;

        for (int day = 1; day <= dayFields.length; day++) {
            try {
                String text = dayFields[day - 1].getText();
                if (!text.isEmpty()) {
                    int value = parseInput(text);

                    totalSum += value;
                    if (!text.equals("4") && !text.equals("5") && !text.equals("6")) {
                        ukupniPutniTroskovi += putniTroskovi;
                        ukupniTopliObrok += topliObrok;
                    }
                    zarada += opcijeVrednosti[Integer.parseInt(text) - 1];
                }
            } catch (NumberFormatException ex) {
                // Skip non-numeric entries
            }
        }

        String message = String.format(
            "Ukupno uplaceno: %d\nZarada: %d\nPutni troskovi: %d\nTopli obrok: %d",
            totalSum, zarada, ukupniPutniTroskovi, ukupniTopliObrok
        );
        JOptionPane.showMessageDialog(this, message);
    }

    private void calculateYearlySum() {
        int totalSum = 0;
        int zarada = 0;
        int ukupniPutniTroskovi = 0;
        int ukupniTopliObrok = 0;

        for (int month = 1; month <= 12; month++) {
            String key = currentYear + "-" + month;
            Map<Integer, String> monthlyEntries = zarade.getOrDefault(key, new HashMap<>());
            for (String text : monthlyEntries.values()) {
                try {
                    if (!text.isEmpty()) {
                        int value = parseInput(text);

                        totalSum += value;
                        if (!text.equals("4") && !text.equals("5") && !text.equals("6")) {
                            ukupniPutniTroskovi += putniTroskovi;
                            ukupniTopliObrok += topliObrok;
                        }
                        zarada += opcijeVrednosti[Integer.parseInt(text) - 1];
                    }
                } catch (NumberFormatException ex) {
                    // Skip non-numeric entries
                }
            }
        }

        String message = String.format(
            "Ukupno uplaceno: %d\nZarada: %d\nPutni troskovi: %d\nTopli obrok: %d",
            totalSum, zarada, ukupniPutniTroskovi, ukupniTopliObrok
        );
        JOptionPane.showMessageDialog(this, message);
    }

    private void resetEntries() {
        for (JTextField dayField : dayFields) {
            dayField.setText("");
        }
        zarade.remove(getCurrentKey());
        saveZarade();
    }

    private int parseInput(String input) {
        try {
            int num = Integer.parseInt(input);
            int value = opcijeVrednosti[num - 1];
            if (num != 4 && num != 5 && num != 6) {
                value += putniTroskovi + topliObrok;
            }
            return value;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void saveCurrentEntries() {
        if (dayFields == null) {
            return;
        }
        String key = getCurrentKey();
        Map<Integer, String> entries = zarade.getOrDefault(key, new HashMap<>());
        for (int day = 1; day <= dayFields.length; day++) {
            String text = dayFields[day - 1].getText();
            if (!text.isEmpty()) {
                entries.put(day, text);
            } else {
                entries.remove(day); // Remove entry if the field is empty
            }
        }
        zarade.put(key, entries);
        saveZarade();
    }

    private void loadZarade() {
        File file = new File(DATA_FILE);
        if (!file.exists()) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(DATA_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":", 4);
                if (parts.length == 4) {
                    String key = parts[0] + "-" + parts[1];
                    int day = Integer.parseInt(parts[2]);
                    String value = parts[3];
                    zarade.putIfAbsent(key, new HashMap<>());
                    zarade.get(key).put(day, value);
                }
            }
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
        }
    }

    private void saveZarade() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(DATA_FILE))) {
            for (Map.Entry<String, Map<Integer, String>> entry : zarade.entrySet()) {
                String[] keyParts = entry.getKey().split("-");
                String year = keyParts[0];
                String month = keyParts[1];
                for (Map.Entry<Integer, String> dayEntry : entry.getValue().entrySet()) {
                    writer.write(year + ":" + month + ":" + dayEntry.getKey() + ":" + dayEntry.getValue());
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadSettings() {
        File file = new File(SETTINGS_FILE);
        if (!file.exists()) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(SETTINGS_FILE))) {
            String line;
            int i = 0;
            while ((line = reader.readLine()) != null && i < opcijeVrednosti.length) {
                opcijeVrednosti[i] = Integer.parseInt(line);
                i++;
            }
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
        }
    }

    private void saveSettings() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(SETTINGS_FILE))) {
            for (int value : opcijeVrednosti) {
                writer.write(String.valueOf(value));
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RadniKalendar().setVisible(true));
    }
}
