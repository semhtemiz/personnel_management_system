package com.semihtemiz.pms.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import com.semihtemiz.pms.model.Employee;
import com.semihtemiz.pms.model.LeaveRequest;
import com.semihtemiz.pms.model.TimeEntry;
import com.semihtemiz.pms.service.DataStorageService;
import com.toedter.calendar.JDateChooser;

/**
 *personel yönetim sistemi'nin ana ekranı
 * 
 *İki farklı görünüm oluşturdum, yönetici ve personel
 *yönetici:
 *personel yönetimi
 *giriş/çıkış takibi
 *izin yönetimi
 *raporlama
 * 
 *personel:
 *kişisel bilgiler
 *kazanç bilgileri
 *giriş/çıkış işlemi
 *izin talebi
 *raporlama
 */
public class MainFrame extends JFrame {
    //veri depolama servisi
    private final DataStorageService dataService;
    
    //aktif kullanıcı bilgisi
    private final Employee currentUser;
    
    //personel listesi tablosu
    private JTable employeeTable;
    private DefaultTableModel employeeTableModel;
    
    //giriş/çıkış kayıtları tablosu
    private JTable timeEntryTable;
    private DefaultTableModel timeEntryTableModel;
    
    //izin talepleri tablosu
    private JTable leaveRequestTable;
    
    //çalışma süresi sayacı
    private Timer workTimer;
    private JLabel timerLabel;
    
    // aktif giriş kaydı
    private TimeEntry currentTimeEntry;

    /**
     *ana pencereyi oluşturma
     *kullanıcı türne göre uygun arayüz
     * 
     * @param dataService veri depolama servisi
     * @param currentUser giriş yapan kullanıcı
     */
    public MainFrame(DataStorageService dataService, Employee currentUser) {
        this.dataService = dataService;
        this.currentUser = currentUser;

        // Ana pencerenin varsayılan ayarları
        setTitle("Personel Yönetim Sistemi");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        // üst bilgi panelini oluşturma
        createHeaderPanel();

        // sekme paneli oluşturma
        JTabbedPane tabbedPane = new JTabbedPane();

        // giriş yapan kullanıcı türüne göre ana arayüzü ayarlama
        if (currentUser.getRole() == Employee.UserRole.ADMIN) {
            createAdminTabs(tabbedPane);
        } else {
            createEmployeeTabs(tabbedPane);
        }

        add(tabbedPane);
    }

    /**
     *üst bilgi paneli:
     *
     *kullanıcı bilgisi (yönetici/personel adı)
     *çalışma süresi sayacı (sadece personel için)
     *çıkış butonu
     */
    private void createHeaderPanel() {
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        //kullanıcı bilgisi
        String userInfo = currentUser.getRole() == Employee.UserRole.ADMIN ?
                "Yönetici" : currentUser.getFullName();
        JLabel userLabel = new JLabel(userInfo);
        headerPanel.add(userLabel);

        //personel için çalışma süresi sayacı
        if (currentUser.getRole() == Employee.UserRole.EMPLOYEE) {
            timerLabel = new JLabel("Çalışma Süresi: 00:00:00");
            headerPanel.add(timerLabel);

            //aktif giriş kaydı kontrolü
            List<TimeEntry> entries = dataService.getTimeEntriesByEmployeeId(currentUser.getId());
            TimeEntry activeEntry = entries.stream()
                    .filter(entry -> entry.getCheckOut() == null)
                    .findFirst()
                    .orElse(null);

            //aktif giriş varsa sayacı başlat
            if (activeEntry != null) {
                currentTimeEntry = activeEntry;
                if (workTimer != null) {
                    workTimer.cancel();
                }
                workTimer = new Timer();
                workTimer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        updateTimer();
                    }
                }, 0, 1000);
            }
        }

        //çıkış butonu
        JButton logoutButton = new JButton("Çıkış Yap");
        logoutButton.addActionListener(e -> handleLogout());
        headerPanel.add(logoutButton);

        add(headerPanel, BorderLayout.NORTH);
    }

    /**
     *yönetici arayüzü:
     *
     *personeller: personel listesi ve yönetimi
     *giriş/Çıkış Takibi: tüm personelin giriş/çıkış kayıtları
     *izin Yönetimi: izin taleplerini görüntüleme ve onaylama
     *raporlar: iş yönetimi ile ilgili bazı raporlar
     * 
     * @param tabbedPane sekmelerin ekleneceği panel
     */
    private void createAdminTabs(JTabbedPane tabbedPane) {
        tabbedPane.addTab("Personeller", createEmployeePanel());
        tabbedPane.addTab("Giriş/Çıkış Takibi", createTimeEntryPanel());
        tabbedPane.addTab("İzin Yönetimi", createLeaveManagementPanel());
        tabbedPane.addTab("Raporlar", createAdminReportPanel());
    }

    /**
     *personel arayüzü:
     *
     *bilgilerim: kişisel bilgiler
     *kazancım: maaş ve ek ödemeler
     *giriş/Çıkış: giriş/çıkış kayıtları ve mesai takibi
     *izin Talebi: izin talep formu ve talep geçmişi
     *raporlar: kişisel raporlar
     *
     * @param tabbedPane sekmelerin ekleneceği panel
     */
    private void createEmployeeTabs(JTabbedPane tabbedPane) {
        tabbedPane.addTab("Bilgilerim", new JScrollPane(createMyInfoPanel()));
        tabbedPane.addTab("Kazancım", createEarningsPanel());
        tabbedPane.addTab("Giriş/Çıkış", createTimeEntryViewPanel());
        tabbedPane.addTab("İzin Talebi", createLeaveRequestPanel());
        tabbedPane.addTab("Raporlar", createEmployeeReportPanel());
    }

    /**
     *personel bilgileri paneli
     *
     *kişisel bilgiler:
     *ad, soyad, tc no
     *iş bilgileri:
     *departman, pozisyon
     *iletişim bilgileri:
     *e-posta, telefon
     *istihdam bilgileri:
     *başlama tarihi, maaş
     *
     * @return bilgileri içeren panel
     */
    private JPanel createMyInfoPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 20, 10, 20);
        gbc.anchor = GridBagConstraints.WEST;

        //panel başlığı ve kenarlık
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(20, 20, 20, 20),
            BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Personel Bilgileri",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 14)
            )
        ));

        //font ayarları
        Font labelFont = new Font("Arial", Font.BOLD, 14);
        Font valueFont = new Font("Arial", Font.PLAIN, 14);

        //bilgileri sırayla ekle
        int row = 0;
        addLabelAndValue(panel, gbc, row++, "Ad:", currentUser.getFirstName(), labelFont, valueFont);
        addLabelAndValue(panel, gbc, row++, "Soyad:", currentUser.getLastName(), labelFont, valueFont);
        addLabelAndValue(panel, gbc, row++, "TC No:", currentUser.getTcNo(), labelFont, valueFont);
        addLabelAndValue(panel, gbc, row++, "Departman:", currentUser.getDepartment(), labelFont, valueFont);
        addLabelAndValue(panel, gbc, row++, "Pozisyon:", currentUser.getPosition(), labelFont, valueFont);
        addLabelAndValue(panel, gbc, row++, "E-posta:", currentUser.getEmail(), labelFont, valueFont);
        addLabelAndValue(panel, gbc, row++, "Telefon:", currentUser.getPhone(), labelFont, valueFont);
        addLabelAndValue(panel, gbc, row++, "İşe Başlama Tarihi:",
                currentUser.getStartDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")), labelFont, valueFont);
        addLabelAndValue(panel, gbc, row++, "Temel Maaş:",
                String.format("%.2f TL", currentUser.getSalary()), labelFont, valueFont);

        mainPanel.add(panel, BorderLayout.WEST);
        return mainPanel;
    }

    /**
     *etiket ve değer çiftini panele ekleme
     *
     * @param panel hedef panel
     * @param gbc grid kısıtlamaları
     * @param row satır numarası
     * @param labelText etiket metni
     * @param value değer metni
     * @param labelFont etiket fontu
     * @param valueFont değer fontu
     */
    private void addLabelAndValue(JPanel panel, GridBagConstraints gbc, int row,
            String labelText, String value, Font labelFont, Font valueFont) {
        gbc.gridx = 0;
        gbc.gridy = row;
        JLabel label = new JLabel(labelText);
        label.setFont(labelFont);
        panel.add(label, gbc);

        gbc.gridx = 1;
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(valueFont);
        panel.add(valueLabel, gbc);
    }

    /**
     *kazanç bilgileri paneli
     *
     *temel maaş
     *fazla mesai ücreti
     *kesintiler
     *toplam kazanç
     *
     * @return kazanç bilgilerini içeren panel
     */
    private JPanel createEarningsPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 20, 10, 20);
        gbc.anchor = GridBagConstraints.WEST;

        //panel başlığı ve kenarlık
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(20, 20, 20, 20),
            BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Kazanç Bilgileri",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 14)
            )
        ));

        //font ayarları
        Font labelFont = new Font("Arial", Font.BOLD, 14);
        Font valueFont = new Font("Arial", Font.PLAIN, 14);

        int row = 0;

        //kazanç bilgilerini sırayla ekle
        addLabelAndValue(panel, gbc, row++, "Temel Maaş:",
            String.format("%.2f TL", currentUser.getSalary()), labelFont, valueFont);

        double overtimeEarnings = calculateOvertimeEarnings();
        addLabelAndValue(panel, gbc, row++, "Fazla Mesai Ücreti:",
            String.format("%.2f TL", overtimeEarnings), labelFont, valueFont);

        double deductions = calculateDeductions();
        addLabelAndValue(panel, gbc, row++, "Kesintiler:",
            String.format("%.2f TL", deductions), labelFont, valueFont);

        double totalSalary = currentUser.getSalary() + overtimeEarnings - deductions;
        addLabelAndValue(panel, gbc, row++, "Toplam Kazanç:",
            String.format("%.2f TL", totalSalary), labelFont, valueFont);

        mainPanel.add(panel, BorderLayout.WEST);
        return mainPanel;
    }

    /**
     *fazla mesai kazancını hesaplama
     *
     *hesaplama:
     *8 saatten fazla çalışma
     *saatlik ücret * 1.5 * fazla mesai saati
     *
     * @return fazla mesai ücreti
     */
    private double calculateOvertimeEarnings() {
        double overtimeHours = 0;
        List<TimeEntry> entries = dataService.getTimeEntriesByEmployeeId(currentUser.getId());
        LocalDate today = LocalDate.now();

        for (TimeEntry entry : entries) {
            if (entry.getCheckOut() != null &&
                entry.getCheckIn().toLocalDate().getMonth() == today.getMonth() &&
                entry.isOvertime()) {

                Duration workDuration = entry.getWorkDuration();
                double hoursWorked = workDuration.toHours();
                if (hoursWorked > 8) {
                    overtimeHours += hoursWorked - 8;
                }
            }
        }

        // fazla mesai ücreti = saatlik ücret * 1.5 * fazla mesai saati
        // saatlik ücret = aylık maaş / (21 gün * 8 saat)
        double hourlyRate = currentUser.getSalary() / (21.0 * 8.0);
        return hourlyRate * 1.5 * overtimeHours;
    }

    /**
     *eksik mesai kesintisini hesaplama
     *
     *hesaplama:
     *8 saatten az çalışma
     *saatlik ücret * eksik saat
     *
     * @return kesinti miktarı
     */
    private double calculateDeductions() {
        double missingHours = 0;
        List<TimeEntry> entries = dataService.getTimeEntriesByEmployeeId(currentUser.getId());
        LocalDate today = LocalDate.now();

        for (TimeEntry entry : entries) {
            if (entry.getCheckOut() != null &&
                entry.getCheckIn().toLocalDate().getMonth() == today.getMonth()) {

                Duration workDuration = entry.getWorkDuration();
                double hoursWorked = workDuration.toHours();
                if (hoursWorked < 8) {
                    missingHours += 8 - hoursWorked;
                }
            }
        }

        // kesinti = saatlik ücret * eksik saat
        // saatlik ücret = aylık maaş / (21 gün * 8 saat)
        double hourlyRate = currentUser.getSalary() / (21.0 * 8.0);
        return hourlyRate * missingHours;
    }

    /**
     *giriş/çıkış takip paneli
     *
     *giriş/çıkış kayıtları tablosu
     *giriş/çıkış butonları
     *çalışma süresi bilgisi
     *
     * @return giriş/çıkış paneli
     */
    private JPanel createTimeEntryViewPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        //tablo modeli
        String[] columns = {"Tarih", "Giriş Saati", "Çıkış Saati", "Çalışma Süresi", "Fazla Mesai"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        timeEntryTableModel = model;
        JTable table = new JTable(model);
        refreshTimeEntryTable();

        //giriş/çıkış butonları
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        JButton startDayButton = new JButton("Günü Başlat");
        JButton endDayButton = new JButton("Günü Bitir");

        startDayButton.setFont(new Font("Arial", Font.BOLD, 12));
        endDayButton.setFont(new Font("Arial", Font.BOLD, 12));

        startDayButton.addActionListener(e -> handleCheckIn());
        endDayButton.addActionListener(e -> handleCheckOut());

        buttonPanel.add(startDayButton);
        buttonPanel.add(endDayButton);

        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    /**
     *giriş/çıkış tablosunu güncelleme
     */
    private void refreshTimeEntryTable() {
        timeEntryTableModel.setRowCount(0);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        List<TimeEntry> entries;
        String[] columns;

        // kullanıcı rolüne göre tablo yapısını belirle
        if (currentUser.getRole() == Employee.UserRole.ADMIN) {
            entries = dataService.getAllTimeEntries();
            columns = new String[]{"Personel ID", "Personel Adı", "Tarih", "Giriş Saati", "Çıkış Saati", "Çalışma Süresi", "Fazla Mesai"};
            timeEntryTableModel.setColumnIdentifiers(columns);
        } else {
            entries = dataService.getTimeEntriesByEmployeeId(currentUser.getId());
            columns = new String[]{"Tarih", "Giriş Saati", "Çıkış Saati", "Çalışma Süresi", "Fazla Mesai"};
            timeEntryTableModel.setColumnIdentifiers(columns);
        }

        // kayıtları tabloya ekle
        for (TimeEntry entry : entries) {
            String checkIn = entry.getCheckIn().format(formatter);
            String checkOut = entry.getCheckOut() != null ? entry.getCheckOut().format(formatter) : "-";
            String duration = entry.getWorkDuration() != null ?
                    entry.getWorkDuration().toHours() + " saat " +
                    (entry.getWorkDuration().toMinutes() % 60) + " dakika" : "-";
            String overtime = entry.isOvertime() ? "Evet" : "Hayır";

            if (currentUser.getRole() == Employee.UserRole.ADMIN) {
                Employee employee = dataService.getEmployeeById(entry.getEmployeeId());
                String employeeName = employee != null ? employee.getFullName() : "Bilinmeyen";

                timeEntryTableModel.addRow(new Object[]{
                        entry.getEmployeeId(),
                        employeeName,
                        entry.getCheckIn().toLocalDate(),
                        checkIn,
                        checkOut,
                        duration,
                        overtime
                });
            } else {
                timeEntryTableModel.addRow(new Object[]{
                        entry.getCheckIn().toLocalDate(),
                        checkIn,
                        checkOut,
                        duration,
                        overtime
                });
            }
        }
    }

    /**
     *giriş kaydı oluşturma
     *
     *aktif giriş kontrolü
     *yeni kayıt oluşturma
     *süre sayacı başlatma
     */
    private void handleCheckIn() {
        // aktif giriş kaydı kontrolü
        List<TimeEntry> entries = dataService.getTimeEntriesByEmployeeId(currentUser.getId());
        boolean hasActiveEntry = entries.stream()
                .anyMatch(entry -> entry.getCheckOut() == null);
        
        if (hasActiveEntry) {
            JOptionPane.showMessageDialog(this,
                "Aktif bir giriş kaydınız bulunmaktadır!\nÖnce mevcut günü bitirmelisiniz.",
                "Uyarı",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        // yeni giriş kaydı oluşturma
        TimeEntry timeEntry = new TimeEntry(
                UUID.randomUUID().toString(),
                currentUser.getId(),
                LocalDateTime.now()
        );
        currentTimeEntry = timeEntry;
        dataService.addTimeEntry(timeEntry);
        refreshTimeEntryTable();
        
        // çalışma süresi sayacını başlat
        if (workTimer != null) {
            workTimer.cancel();
        }
        workTimer = new Timer();
        workTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updateTimer();
            }
        }, 0, 1000);
    }

    /**
     *çıkış kaydı oluşturma
     *
     *aktif giriş bulma
     *çıkış zamanı kaydetme
     *süre sayacı durdurma
     */
    private void handleCheckOut() {
        List<TimeEntry> entries = dataService.getTimeEntriesByEmployeeId(currentUser.getId());
        TimeEntry lastEntry = null;
        for (TimeEntry entry : entries) {
            if (entry.getCheckOut() == null) {
                lastEntry = entry;
                break;
            }
        }

        if (lastEntry != null) {
            lastEntry.setCheckOut(LocalDateTime.now());
            dataService.updateTimeEntry(lastEntry);
            refreshTimeEntryTable();
            
            // sayacı durdur
            if (workTimer != null) {
                workTimer.cancel();
                workTimer = null;
            }
            if (timerLabel != null) {
                timerLabel.setText("Çalışma Süresi: 00:00:00");
            }
            currentTimeEntry = null;
        } else {
            JOptionPane.showMessageDialog(this, "Aktif giriş kaydı bulunamadı!");
        }
    }

    /**
     *çalışma süresi sayacını güncelleme
     *
     *hesaplama:
     *giriş zamanından şu ana kadar geçen süre
     *saat:dakika:saniye olarak gösterme
     */
    private void updateTimer() {
        if (currentTimeEntry != null) {
            LocalDateTime now = LocalDateTime.now();
            Duration duration = Duration.between(currentTimeEntry.getCheckIn(), now);
            long hours = duration.toHours();
            long minutes = duration.toMinutesPart();
            long seconds = duration.toSecondsPart();
            
            SwingUtilities.invokeLater(() -> {
                timerLabel.setText(String.format("Çalışma Süresi: %02d:%02d:%02d", 
                    hours, minutes, seconds));
            });
        }
    }

    /**
     *personel raporlama panelini oluşturma
     *
     *rapor türleri:
     *çalışma saatleri
     *izin kullanımı
     *fazla mesai
     *eksik mesai
     *
     * @return raporlama paneli
     */
    private JPanel createEmployeeReportPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        // rapor türü seçimi
        JComboBox<String> reportType = new JComboBox<>(new String[]{
            "Çalışma Saatleri",
            "İzin Kullanımı",
            "Fazla Mesai",
            "Eksik Mesai"
        });

        // tarih aralığı seçimi
        JDateChooser startDateChooser = new JDateChooser();
        JDateChooser endDateChooser = new JDateChooser();
        startDateChooser.setDateFormatString("dd.MM.yyyy");
        endDateChooser.setDateFormatString("dd.MM.yyyy");
        
        // varsayılan tarihler (içinde bulunulan ay)
        startDateChooser.setDate(java.sql.Date.valueOf(LocalDate.now().withDayOfMonth(1)));
        endDateChooser.setDate(java.sql.Date.valueOf(LocalDate.now()));

        // rapor oluşturma butonu
        JButton generateButton = new JButton("Rapor Oluştur");

        // kontrol paneli bileşenlerini ekle
        controlPanel.add(new JLabel("Rapor Türü:"));
        controlPanel.add(reportType);
        controlPanel.add(new JLabel("Başlangıç:"));
        controlPanel.add(startDateChooser);
        controlPanel.add(new JLabel("Bitiş:"));
        controlPanel.add(endDateChooser);
        controlPanel.add(generateButton);

        // rapor görüntüleme alanı
        JTextArea reportArea = new JTextArea();
        reportArea.setEditable(false);

        // rapor oluşturma işlemi
        generateButton.addActionListener(e -> {
            try {
                LocalDate startDate = startDateChooser.getDate().toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate();
                LocalDate endDate = endDateChooser.getDate().toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate();
                String report = generateEmployeeReport(
                    reportType.getSelectedIndex(),
                    startDate,
                    endDate
                );
                reportArea.setText(report);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                    "Rapor oluşturulurken hata: " + ex.getMessage(),
                    "Hata",
                    JOptionPane.ERROR_MESSAGE);
            }
        });

        panel.add(controlPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(reportArea), BorderLayout.CENTER);

        return panel;
    }

    /**
     *personel raporu oluşturma
     *
     *rapor içerikleri:
     *çalışma saatleri: günlük çalışma ve fazla mesailer
     *izin kullanımı: talepler ve kalan günler
     *fazla mesai: süreler ve kazançlar
     *eksik mesai: süreler ve kesintiler
     *
     * @param reportType rapor türü
     * @param startDate başlangıç tarihi
     * @param endDate bitiş tarihi
     * @return rapor metni
     */
    private String generateEmployeeReport(int reportType, LocalDate startDate, LocalDate endDate) {
        StringBuilder report = new StringBuilder();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        switch (reportType) {
            case 0: // çalışma Saatleri
                report.append("ÇALIŞMA SAATLERİ RAPORU\n");
                report.append("Dönem: ").append(startDate.format(dateFormatter))
                      .append(" - ").append(endDate.format(dateFormatter)).append("\n\n");

                List<TimeEntry> entries = dataService.getTimeEntriesByEmployeeId(currentUser.getId());
                long totalHours = 0;
                long overtimeHours = 0;

                // her gün için çalışma detaylarını ekle
                for (TimeEntry entry : entries) {
                    if (entry.getCheckOut() != null &&
                        !entry.getCheckIn().toLocalDate().isBefore(startDate) &&
                        !entry.getCheckIn().toLocalDate().isAfter(endDate)) {

                        report.append(entry.getCheckIn().toLocalDate().format(dateFormatter)).append(":\n");
                        Duration duration = entry.getWorkDuration();
                        long hours = duration.toHours();
                        long minutes = duration.toMinutesPart();

                        report.append("  Giriş: ").append(entry.getCheckIn().format(DateTimeFormatter.ofPattern("HH:mm"))).append("\n");
                        report.append("  Çıkış: ").append(entry.getCheckOut().format(DateTimeFormatter.ofPattern("HH:mm"))).append("\n");
                        report.append("  Süre: ").append(hours).append(" saat ").append(minutes).append(" dakika\n");

                        if (hours > 8) {
                            report.append("  Fazla Mesai: ").append(hours - 8).append(" saat\n");
                            overtimeHours += hours - 8;
                        }
                        report.append("\n");

                        totalHours += hours;
                    }
                }

                // toplam süreleri ekle
                report.append("ÖZET:\n");
                report.append("Toplam Çalışma: ").append(totalHours).append(" saat\n");
                report.append("Toplam Fazla Mesai: ").append(overtimeHours).append(" saat\n");
                break;

            case 1: // izin Kullanımı
                report.append("İZİN KULLANIM RAPORU\n");
                report.append("Dönem: ").append(startDate.format(dateFormatter))
                      .append(" - ").append(endDate.format(dateFormatter)).append("\n\n");

                List<LeaveRequest> leaves = dataService.getLeaveRequestsByEmployeeId(currentUser.getId());
                int totalVacationDays = 30; // Sabit 30 gün izin hakkı
                int usedDays = dataService.getUsedLeaveDays(currentUser.getId(), LocalDate.now().getYear());
                int pendingDays = 0;

                // izin taleplerini listele
                report.append("İzin Talepleri:\n\n");
                for (LeaveRequest leave : leaves) {
                    if (!leave.getStartDate().isBefore(startDate) &&
                        !leave.getEndDate().isAfter(endDate)) {

                        report.append("Tarih: ").append(leave.getStartDate().format(dateFormatter))
                              .append(" - ").append(leave.getEndDate().format(dateFormatter)).append("\n");
                        report.append("Süre: ").append(leave.getDurationInDays()).append(" gün\n");
                        report.append("Durum: ").append(leave.getStatus().getDisplayName()).append("\n");
                        if (leave.getApproverNotes() != null && !leave.getApproverNotes().isEmpty()) {
                            report.append("Notlar: ").append(leave.getApproverNotes()).append("\n");
                        }
                        report.append("\n");

                        if (leave.getStatus() == LeaveRequest.LeaveStatus.PENDING) {
                            pendingDays += leave.getDurationInDays();
                        }
                    }
                }

                // izin durumunu özetle
                report.append("ÖZET:\n");
                report.append("Toplam İzin Hakkı: ").append(totalVacationDays).append(" gün\n");
                report.append("Kullanılan İzin: ").append(usedDays).append(" gün\n");
                report.append("Bekleyen İzin: ").append(pendingDays).append(" gün\n");
                report.append("Kalan İzin: ").append(totalVacationDays - usedDays).append(" gün\n");
                break;

            case 2: // fazla Mesai
                report.append("FAZLA MESAİ RAPORU\n");
                report.append("Dönem: ").append(startDate.format(dateFormatter))
                      .append(" - ").append(endDate.format(dateFormatter)).append("\n\n");

                entries = dataService.getTimeEntriesByEmployeeId(currentUser.getId());
                double totalEarnings = 0;

                // fazla mesai detaylarını listele
                for (TimeEntry entry : entries) {
                    if (entry.getCheckOut() != null &&
                        !entry.getCheckIn().toLocalDate().isBefore(startDate) &&
                        !entry.getCheckIn().toLocalDate().isAfter(endDate) &&
                        entry.isOvertime()) {

                        report.append(entry.getCheckIn().toLocalDate().format(dateFormatter)).append(":\n");
                        Duration duration = entry.getWorkDuration();
                        long hours = duration.toHours();

                        if (hours > 8) {
                            long overtime = hours - 8;
                            double earnings = overtime * currentUser.getHourlyRate() * 1.5;

                            report.append("  Fazla Mesai: ").append(overtime).append(" saat\n");
                            report.append("  Kazanç: ").append(String.format("%.2f TL", earnings)).append("\n\n");

                            totalEarnings += earnings;
                        }
                    }
                }

                // toplam kazancı ekle
                report.append("ÖZET:\n");
                report.append("Toplam Fazla Mesai Kazancı: ").append(String.format("%.2f TL", totalEarnings)).append("\n");
                break;

            case 3: // eksik Mesai
                report.append("EKSİK MESAİ RAPORU\n");
                report.append("Dönem: ").append(startDate.format(dateFormatter))
                      .append(" - ").append(endDate.format(dateFormatter)).append("\n\n");

                entries = dataService.getTimeEntriesByEmployeeId(currentUser.getId());
                double totalMissingHours = 0;
                double totalDeductions = 0;

                //eksik mesai detaylarını listele
                for (TimeEntry entry : entries) {
                    if (entry.getCheckOut() != null &&
                        !entry.getCheckIn().toLocalDate().isBefore(startDate) &&
                        !entry.getCheckIn().toLocalDate().isAfter(endDate)) {

                        Duration workDuration = entry.getWorkDuration();
                        double hoursWorked = workDuration.toHours();

                        if (hoursWorked < 8) {
                            double missingHours = 8 - hoursWorked;
                            double deduction = missingHours * (currentUser.getSalary() / (21 * 8));

                            report.append(entry.getCheckIn().toLocalDate().format(dateFormatter)).append(":\n");
                            report.append("  Çalışılan: ").append(String.format("%.2f", hoursWorked)).append(" saat\n");
                            report.append("  Eksik: ").append(String.format("%.2f", missingHours)).append(" saat\n");
                            report.append("  Kesinti: ").append(String.format("%.2f TL", deduction)).append("\n\n");

                            totalMissingHours += missingHours;
                            totalDeductions += deduction;
                        }
                    }
                }

                //toplam kesintileri ekle
                report.append("\nÖZET:\n");
                report.append("Toplam Eksik Mesai: ").append(String.format("%.2f", totalMissingHours)).append(" saat\n");
                report.append("Toplam Kesinti: ").append(String.format("%.2f TL", totalDeductions)).append("\n");
                break;
        }

        return report.toString();
    }

    /**
     *izin yönetimi paneli
     *
     *izin talepleri tablosu
     *onaylama/reddetme butonları
     *talep detayları
     *
     * @return izin yönetimi paneli
     */
    private JPanel createLeaveManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // tablo modelini oluşturma
        String[] columns = {"ID", "Personel", "Başlangıç", "Bitiş", "Süre", "Neden", "Durum"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        JTable table = new JTable(model);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        // onay red butonları
        JPanel buttonPanel = new JPanel();
        JButton approveButton = new JButton("Onayla");
        JButton rejectButton = new JButton("Reddet");

        approveButton.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow >= 0) {
                handleLeaveRequestResponse(table, selectedRow, true);
            } else {
                JOptionPane.showMessageDialog(this, "Lütfen bir izin talebi seçin!");
            }
        });

        rejectButton.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow >= 0) {
                handleLeaveRequestResponse(table, selectedRow, false);
            } else {
                JOptionPane.showMessageDialog(this, "Lütfen bir izin talebi seçin!");
            }
        });

        buttonPanel.add(approveButton);
        buttonPanel.add(rejectButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        refreshLeaveRequestTable(model);
        return panel;
    }

    /**
     *izin talebini yanıtlama
     *
     *işlem adımları:
     *seçilen talebi bulma
     *yönetici notu alma
     *talep durumu güncelleme
     *izin günü güncelleme
     *
     * @param table izin talepleri tablosu
     * @param selectedRow seçili satır
     * @param approved onay durumu
     */
    private void handleLeaveRequestResponse(JTable table, int selectedRow, boolean approved) {
        String id = (String) table.getValueAt(selectedRow, 0);
        String employeeInfo = (String) table.getValueAt(selectedRow, 1);
        String employeeId = employeeInfo.split(" - ")[0]; // Get employee ID from the formatted string
        
        // izin talebini bul
        List<LeaveRequest> requests = dataService.getAllLeaveRequests();
        LeaveRequest request = requests.stream()
                .filter(r -> r.getId().equals(id))
                .findFirst()
                .orElse(null);

        if (request != null) {
            // yönetici notunu al
            String notes = JOptionPane.showInputDialog(this,
                    "Notlar:",
                    approved ? "İzin Onaylama" : "İzin Reddetme",
                    JOptionPane.PLAIN_MESSAGE);

            // talep durumunu güncelle
            request.setStatus(approved ? LeaveRequest.LeaveStatus.APPROVED : 
                                      LeaveRequest.LeaveStatus.REJECTED);
            request.setApproverNotes(notes);
            dataService.updateLeaveRequest(request);

            // onaylanırsa izin günlerini güncelle
            if (approved) {
                Employee employee = dataService.getEmployeeById(employeeId);
                if (employee != null) {
                    int duration = request.getDurationInDays();
                    int remainingDays = employee.getVacationDays() - duration;
                    if (remainingDays < 0) {
                        JOptionPane.showMessageDialog(this,
                            "Uyarı: Personelin yeterli izin günü bulunmamaktadır!",
                            "Uyarı",
                            JOptionPane.WARNING_MESSAGE);
                        request.setStatus(LeaveRequest.LeaveStatus.REJECTED);
                        request.setApproverNotes("Yetersiz izin günü");
                        dataService.updateLeaveRequest(request);
                    } else {
                        employee.setVacationDays(remainingDays);
                        dataService.updateEmployee(employee);
                    }
                }
            }

            refreshLeaveRequestTable((DefaultTableModel) table.getModel());
        }
    }

    /**
     *izin talep paneli
     *
     *izin talepleri tablosu
     *yeni talep butonu
     *talep geçmişi
     *
     * @return izin talep paneli
     */
    private JPanel createLeaveRequestPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // tablo modelini oluşturma
        String[] columns = {"Başlangıç", "Bitiş", "Süre", "Neden", "Durum", "Notlar"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        leaveRequestTable = new JTable(model);
        panel.add(new JScrollPane(leaveRequestTable), BorderLayout.CENTER);

        // yeni talep butonu
        JButton newRequestButton = new JButton("Yeni İzin Talebi");
        newRequestButton.addActionListener(e -> showLeaveRequestDialog());

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(newRequestButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        refreshEmployeeLeaveRequestTable(model);
        return panel;
    }

    /**
     *izin talebini gösterme
     */
    private void showLeaveRequestDialog() {
        JDialog dialog = new JDialog(this, "Yeni İzin Talebi", true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Başlangıç tarihi
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Başlangıç Tarihi:"), gbc);
        JDateChooser leaveStartDateChooser = new JDateChooser();
        leaveStartDateChooser.setDateFormatString("dd.MM.yyyy");
        gbc.gridx = 1;
        panel.add(leaveStartDateChooser, gbc);

        // bitiş tarihi
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Bitiş Tarihi:"), gbc);
        JDateChooser leaveEndDateChooser = new JDateChooser();
        leaveEndDateChooser.setDateFormatString("dd.MM.yyyy");
        gbc.gridx = 1;
        panel.add(leaveEndDateChooser, gbc);

        // izin nedeni
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("İzin Nedeni:"), gbc);
        JTextArea reasonArea = new JTextArea(3, 20);
        reasonArea.setLineWrap(true);
        gbc.gridx = 1;
        panel.add(new JScrollPane(reasonArea), gbc);

        // kalan izin günü bilgisi
        int usedDays = dataService.getUsedLeaveDays(currentUser.getId(), LocalDate.now().getYear());
        int remainingDays = currentUser.getVacationDays() - usedDays;
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        panel.add(new JLabel("Kalan İzin Günü: " + remainingDays), gbc);

        // işlem butonları
        JPanel buttonPanel = new JPanel();
        JButton submitButton = new JButton("Gönder");
        JButton cancelButton = new JButton("İptal");

        submitButton.addActionListener(e -> {
            submitLeaveRequest(dialog, leaveStartDateChooser, leaveEndDateChooser, reasonArea, remainingDays);
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(submitButton);
        buttonPanel.add(cancelButton);

        gbc.gridy = 4;
        panel.add(buttonPanel, gbc);

        dialog.add(panel);
        dialog.setVisible(true);
    }

    /**
     *izin talebini kaydetme
     *
     *tarih alanları doluluğu
     *izin nedeni doluluğu
     *kalan izin yeterliliği
     *
     * @param dialog izin talebi dialogu
     * @param startDateChooser başlangıç tarihi seçici
     * @param endDateChooser bitiş tarihi seçici
     * @param reasonArea izin nedeni alanı
     * @param remainingDays kalan izin günü
     */
    private void submitLeaveRequest(JDialog dialog, JDateChooser startDateChooser,
            JDateChooser endDateChooser, JTextArea reasonArea, int remainingDays) {
        try {
            // tarih kontrolü
            if (startDateChooser.getDate() == null || endDateChooser.getDate() == null) {
                throw new IllegalArgumentException("Lütfen başlangıç ve bitiş tarihlerini seçin!");
            }
            
            LocalDate startDate = startDateChooser.getDate().toInstant()
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate();
            
            LocalDate endDate = endDateChooser.getDate().toInstant()
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate();
            
            String reason = reasonArea.getText();

            // izin nedeni kontrolü
            if (reason.trim().isEmpty()) {
                throw new IllegalArgumentException("İzin nedeni boş olamaz!");
            }

            // izin günü kontrolü
            int duration = (int) startDate.until(endDate.plusDays(1)).getDays();
            if (duration > remainingDays) {
                throw new IllegalArgumentException("Yeterli izin gününüz bulunmamaktadır!");
            }

            //izin talebini oluştur ve kaydet
            LeaveRequest request = new LeaveRequest(
                UUID.randomUUID().toString(),
                currentUser.getId(),
                startDate,
                endDate,
                reason
            );

            dataService.addLeaveRequest(request);
            dialog.dispose();
            refreshEmployeeLeaveRequestTable((DefaultTableModel) leaveRequestTable.getModel());
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(dialog,
                "Hata: " + e.getMessage(),
                "Hata",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     *izin talepleri tablosunu güncelleme
     *
     *personel bilgisi
     *tarih bilgileri
     *izin süresi
     *talep durumu
     *
     * @param model tablo modeli
     */
    private void refreshLeaveRequestTable(DefaultTableModel model) {
        model.setRowCount(0);
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        
        List<LeaveRequest> requests = dataService.getAllLeaveRequests();
        for (LeaveRequest request : requests) {
            Employee employee = dataService.getEmployeeById(request.getEmployeeId());
            String employeeInfo = employee != null ? 
                String.format("%s %s", employee.getFirstName(), employee.getLastName()) :
                "Bilinmeyen Personel";
            
            model.addRow(new Object[]{
                request.getEmployeeId(),
                employeeInfo,
                request.getStartDate().format(dateFormatter),
                request.getEndDate().format(dateFormatter),
                request.getDurationInDays() + " gün",
                request.getReason(),
                request.getStatus().getDisplayName()
            });
        }
    }

    /**
     *personel izin tablosunu güncelleme
     *
     *tarih bilgileri
     *izin süresi
     *izin nedeni
     *talep durumu
     *yönetici notu
     *
     * @param model tablo modeli
     */
    private void refreshEmployeeLeaveRequestTable(DefaultTableModel model) {
        model.setRowCount(0);
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        
        List<LeaveRequest> requests = dataService.getLeaveRequestsByEmployeeId(currentUser.getId());
        for (LeaveRequest request : requests) {
            model.addRow(new Object[]{
                request.getStartDate().format(dateFormatter),
                request.getEndDate().format(dateFormatter),
                request.getDurationInDays() + " gün",
                request.getReason(),
                request.getStatus().getDisplayName(),
                request.getApproverNotes() != null ? request.getApproverNotes() : "-"
            });
        }
    }

    /**
     *yönetici rapor paneli
     *
     *rapor türü seçimi
     *tarih aralığı seçimi
     *rapor görüntüleme alanı
     *
     * @return rapor paneli
     */
    private JPanel createAdminReportPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        //rapor türü seçme
        JComboBox<String> reportType = new JComboBox<>(new String[]{
            "Personel Çalışma Saatleri",
            "Departman Bazlı Çalışma Analizi",
            "İzin Kullanım Raporu",
            "Fazla Mesai Raporu",
            "Eksik Mesai Raporu"
        });

        JDateChooser startDateChooser = new JDateChooser();
        JDateChooser endDateChooser = new JDateChooser();
        startDateChooser.setDateFormatString("dd.MM.yyyy");
        endDateChooser.setDateFormatString("dd.MM.yyyy");

        startDateChooser.setDate(java.sql.Date.valueOf(LocalDate.now().withDayOfMonth(1)));
        endDateChooser.setDate(java.sql.Date.valueOf(LocalDate.now()));

        JButton generateButton = new JButton("Rapor Oluştur");

        controlPanel.add(new JLabel("Rapor Türü:"));
        controlPanel.add(reportType);
        controlPanel.add(new JLabel("Başlangıç:"));
        controlPanel.add(startDateChooser);
        controlPanel.add(new JLabel("Bitiş:"));
        controlPanel.add(endDateChooser);
        controlPanel.add(generateButton);

        JTextArea reportArea = new JTextArea();
        reportArea.setEditable(false);

        generateButton.addActionListener(e -> {
            try {
                LocalDate startDate = startDateChooser.getDate().toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate();
                LocalDate endDate = endDateChooser.getDate().toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate();
                String report = generateReport(
                    reportType.getSelectedIndex(),
                    startDate,
                    endDate
                );
                reportArea.setText(report);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                    "Rapor oluşturulurken hata: " + ex.getMessage(),
                    "Hata",
                    JOptionPane.ERROR_MESSAGE);
            }
        });

        panel.add(controlPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(reportArea), BorderLayout.CENTER);

        return panel;
    }

    /**
     *yönetici raporu oluşturma
     *
     *personel çalışma saatleri
     *departman bazlı analiz
     *izin kullanım raporu
     *fazla/eksik mesai raporu
     *
     * @param reportType rapor türü (0-4)
     * @param startDate başlangıç tarihi
     * @param endDate bitiş tarihi
     * @return rapor metni
     */
    private String generateReport(int reportType, LocalDate startDate, LocalDate endDate) {
        StringBuilder report = new StringBuilder();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        switch (reportType) {
            case 0: // personel çalışma saatleri
                report.append("PERSONEL ÇALIŞMA SAATLERİ RAPORU\n");
                report.append("Dönem: ").append(startDate.format(dateFormatter))
                      .append(" - ").append(endDate.format(dateFormatter)).append("\n\n");

                for (Employee employee : dataService.getAllEmployees()) {
                    // admin kullanıcısını raporlama dışında tut
                    if (employee.getRole() == Employee.UserRole.ADMIN) {
                        continue;
                    }
                    report.append(employee.getFullName()).append(":\n");
                    List<TimeEntry> entries = dataService.getTimeEntriesByEmployeeId(employee.getId());
                    
                    long totalHours = 0;
                    long overtimeHours = 0;
                    
                    for (TimeEntry entry : entries) {
                        if (entry.getCheckOut() != null &&
                            !entry.getCheckIn().toLocalDate().isBefore(startDate) &&
                            !entry.getCheckIn().toLocalDate().isAfter(endDate)) {
                            
                            Duration duration = entry.getWorkDuration();
                            totalHours += duration.toHours();
                            if (duration.toHours() > 8) {
                                overtimeHours += duration.toHours() - 8;
                            }
                        }
                    }
                    
                    report.append("  Toplam Çalışma: ").append(totalHours).append(" saat\n");
                    report.append("  Toplam Fazla Mesai: ").append(overtimeHours).append(" saat\n\n");
                }
                break;

            case 1: //departman bazlı çalışma analizi
                report.append("DEPARTMAN BAZLI ÇALIŞMA ANALİZİ\n");
                report.append("Dönem: ").append(startDate.format(dateFormatter))
                      .append(" - ").append(endDate.format(dateFormatter)).append("\n\n");

                //personelleri departmanlara göre grupla ve yöneticiyi hariç tut
                Map<String, List<Employee>> departmentEmployees = dataService.getAllEmployees()
                    .stream()
                    .filter(e -> e.getRole() != Employee.UserRole.ADMIN)
                    .collect(Collectors.groupingBy(Employee::getDepartment));

                for (Map.Entry<String, List<Employee>> entry : departmentEmployees.entrySet()) {
                    report.append(entry.getKey()).append(" Departmanı:\n");
                    report.append("  Çalışan Sayısı: ").append(entry.getValue().size()).append("\n");
                    
                    long departmentTotalHours = 0;
                    long departmentOvertimeHours = 0;
                    
                    for (Employee employee : entry.getValue()) {
                        List<TimeEntry> timeEntries = dataService.getTimeEntriesByEmployeeId(employee.getId());
                        for (TimeEntry timeEntry : timeEntries) {
                            if (timeEntry.getCheckOut() != null &&
                                !timeEntry.getCheckIn().toLocalDate().isBefore(startDate) &&
                                !timeEntry.getCheckIn().toLocalDate().isAfter(endDate)) {
                                
                                Duration duration = timeEntry.getWorkDuration();
                                departmentTotalHours += duration.toHours();
                                if (duration.toHours() > 8) {
                                    departmentOvertimeHours += duration.toHours() - 8;
                                }
                            }
                        }
                    }
                    
                    report.append("  Toplam Çalışma: ").append(departmentTotalHours).append(" saat\n");
                    report.append("  Toplam Fazla Mesai: ").append(departmentOvertimeHours).append(" saat\n\n");
                }
                break;

            case 2: // izin kullanım raporu
                report.append("İZİN KULLANIM RAPORU\n");
                report.append("Dönem: ").append(startDate.format(dateFormatter))
                      .append(" - ").append(endDate.format(dateFormatter)).append("\n\n");

                for (Employee employee : dataService.getAllEmployees()) {
                    // admin kullanıcısını raporlama dışında tut
                    if (employee.getRole() == Employee.UserRole.ADMIN) {
                        continue;
                    }
                    report.append(employee.getFullName()).append(":\n");
                    List<LeaveRequest> leaves = dataService.getLeaveRequestsByEmployeeId(employee.getId());
                    
                    int approvedDays = 0;
                    int pendingDays = 0;
                    int rejectedDays = 0;
                    
                    for (LeaveRequest leave : leaves) {
                        if (!leave.getStartDate().isBefore(startDate) &&
                            !leave.getEndDate().isAfter(endDate)) {
                            int days = leave.getDurationInDays();
                            switch (leave.getStatus()) {
                                case APPROVED:
                                    approvedDays += days;
                                    break;
                                case PENDING:
                                    pendingDays += days;
                                    break;
                                case REJECTED:
                                    rejectedDays += days;
                                    break;
                            }
                        }
                    }
                    
                    report.append("  Toplam İzin Hakkı: ").append(employee.getVacationDays()).append(" gün\n");
                    report.append("  Onaylanan İzin: ").append(approvedDays).append(" gün\n");
                    report.append("  Bekleyen İzin: ").append(pendingDays).append(" gün\n");
                    report.append("  Reddedilen İzin: ").append(rejectedDays).append(" gün\n\n");
                }
                break;

            case 3: // fazla mesai raporu
                report.append("FAZLA MESAİ RAPORU\n");
                report.append("Dönem: ").append(startDate.format(dateFormatter))
                      .append(" - ").append(endDate.format(dateFormatter)).append("\n\n");

                for (Employee employee : dataService.getAllEmployees()) {
                    // admin kullanıcısını raporlama dışında tut
                    if (employee.getRole() == Employee.UserRole.ADMIN) {
                        continue;
                    }
                    report.append(employee.getFullName()).append(":\n");
                    List<TimeEntry> entries = dataService.getTimeEntriesByEmployeeId(employee.getId());
                    
                    long overtimeHours = 0;
                    double overtimeEarnings = 0;
                    
                    for (TimeEntry entry : entries) {
                        if (entry.getCheckOut() != null &&
                            !entry.getCheckIn().toLocalDate().isBefore(startDate) &&
                            !entry.getCheckIn().toLocalDate().isAfter(endDate) &&
                            entry.isOvertime()) {
                            
                            Duration duration = entry.getWorkDuration();
                            long hours = duration.toHours();
                            if (hours > 8) {
                                long overtime = hours - 8;
                                overtimeHours += overtime;
                                overtimeEarnings += overtime * employee.getHourlyRate() * 1.5;
                            }
                        }
                    }
                    
                    report.append("  Fazla Mesai: ").append(overtimeHours).append(" saat\n");
                    report.append("  Fazla Mesai Ücreti: ").append(String.format("%.2f TL", overtimeEarnings)).append("\n\n");
                }
                break;

            case 4: // eksik mesai raporu
                report.append("EKSİK MESAİ RAPORU\n");
                report.append("Dönem: ").append(startDate.format(dateFormatter))
                      .append(" - ").append(endDate.format(dateFormatter)).append("\n\n");

                for (Employee employee : dataService.getAllEmployees()) {
                    // admin kullanıcısını raporlama dışında tut
                    if (employee.getRole() == Employee.UserRole.ADMIN) {
                        continue;
                    }
                    
                    List<TimeEntry> entries = dataService.getTimeEntriesByEmployeeId(employee.getId());
                    long totalMissingHours = 0;
                    double totalDeductions = 0;
                    
                    report.append(employee.getFullName()).append(":\n");
                    
                    for (TimeEntry entry : entries) {
                        if (entry.getCheckOut() != null &&
                            !entry.getCheckIn().toLocalDate().isBefore(startDate) &&
                            !entry.getCheckIn().toLocalDate().isAfter(endDate)) {
                            
                            Duration duration = entry.getWorkDuration();
                            long hours = duration.toHours();
                            
                            if (hours < 8) {
                                long missingHours = 8 - hours;
                                double deduction = missingHours * employee.getHourlyRate();
                                
                                report.append("  ").append(entry.getCheckIn().toLocalDate().format(dateFormatter)).append(":\n");
                                report.append("    Çalışılan: ").append(hours).append(" saat\n");
                                report.append("    Eksik: ").append(missingHours).append(" saat\n");
                                report.append("    Kesinti: ").append(String.format("%.2f TL", deduction)).append("\n");
                                
                                totalMissingHours += missingHours;
                                totalDeductions += deduction;
                            }
                        }
                    }
                    
                    if (totalMissingHours > 0) {
                        report.append("\n  ÖZET:\n");
                        report.append("  Toplam Eksik Mesai: ").append(totalMissingHours).append(" saat\n");
                        report.append("  Toplam Kesinti: ").append(String.format("%.2f TL", totalDeductions)).append("\n");
                    } else {
                        report.append("  Eksik mesai bulunmamaktadır.\n");
                    }
                    report.append("\n");
                }
                break;
        }

        return report.toString();
    }

    /**
     *oturumu kapatma
     *
     *personel ise sayacı durdur
     *giriş ekranına döner
     *mevcut pencereyi kapatır
     */
    private void handleLogout() {
        // personel ise sayacı durdur ama çıkış kaydı oluşturma
        if (currentUser.getRole() == Employee.UserRole.EMPLOYEE) {
            if (workTimer != null) {
                workTimer.cancel();
            }
        }

        // giriş ekranını göster
        LoginFrame loginFrame = new LoginFrame(dataService);
        loginFrame.setVisible(true);
        dispose();
    }

    /**
     *personel yönetim paneli
     *
     *personel listesi tablosu
     *ekleme/düzenleme/silme butonları
     *personel detayları
     *
     * @return personel yönetim paneli
     */
    private JPanel createEmployeePanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // tablo modelini oluşturma
        String[] columns = {"ID", "Ad", "Soyad", "TC No", "Departman", "Pozisyon", 
                          "E-posta", "Telefon", "Maaş", "İşe Başlama Tarihi"};
        employeeTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        employeeTable = new JTable(employeeTableModel);
        JScrollPane scrollPane = new JScrollPane(employeeTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        // buton panelini oluşturma
        JPanel buttonPanel = new JPanel();
        JButton addButton = new JButton("Yeni Personel");
        JButton editButton = new JButton("Düzenle");
        JButton deleteButton = new JButton("Sil");

        addButton.addActionListener(e -> showEmployeeDialog(null));
        editButton.addActionListener(e -> {
            int selectedRow = employeeTable.getSelectedRow();
            if (selectedRow >= 0) {
                String id = (String) employeeTableModel.getValueAt(selectedRow, 0);
                Employee employee = dataService.getEmployeeById(id);
                showEmployeeDialog(employee);
            } else {
                JOptionPane.showMessageDialog(this, "Lütfen bir personel seçin!");
            }
        });
        deleteButton.addActionListener(e -> {
            int selectedRow = employeeTable.getSelectedRow();
            if (selectedRow >= 0) {
                String id = (String) employeeTableModel.getValueAt(selectedRow, 0);
                int result = JOptionPane.showConfirmDialog(this,
                        "Personeli silmek istediğinizden emin misiniz?",
                        "Personel Silme",
                        JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                    dataService.deleteEmployee(id);
                    refreshEmployeeTable();
                }
            } else {
                JOptionPane.showMessageDialog(this, "Lütfen bir personel seçin!");
            }
        });

        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        refreshEmployeeTable();
        return panel;
    }

    /**
     *personel tablosunu güncelleme
     *
     *kişisel bilgiler
     *iş bilgileri
     *iletişim bilgileri
     */
    private void refreshEmployeeTable() {
        DefaultTableModel model = (DefaultTableModel) employeeTable.getModel();
        model.setRowCount(0);
        
        // yönetici hariç tüm personeli listele
        List<Employee> employees = dataService.getAllEmployees().stream()
            .filter(e -> e.getRole() != Employee.UserRole.ADMIN)
            .collect(Collectors.toList());

        for (Employee employee : employees) {
            model.addRow(new Object[]{
                employee.getId(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getTcNo(),
                employee.getDepartment(),
                employee.getPosition(),
                employee.getEmail(),
                employee.getPhone(),
                employee.getSalary(),
                employee.getStartDate()
            });
        }
    }

    /**
     *giriş/çıkış takip paneli
     *
     *giriş/çıkış kayıtları tablosu
     *tarih ve saat bilgileri
     *çalışma süreleri
     *
     * @return giriş/çıkış paneli
     */
    private JPanel createTimeEntryPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // tablo modelini oluşturma
        String[] columns = {"Tarih", "Giriş Saati", "Çıkış Saati", "Çalışma Süresi", "Fazla Mesai"};
        timeEntryTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        timeEntryTable = new JTable(timeEntryTableModel);
        JScrollPane scrollPane = new JScrollPane(timeEntryTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        refreshTimeEntryTable();
        return panel;
    }

    /**
     *personel bilgileri gösterme
     *
     *dialog içeriği:
     *kişisel bilgi alanları
     *iş bilgi alanları
     *iletişim bilgi alanları
     *sistem bilgi alanları
     *
     * @param employee düzenlenecek personel
     */
    private void showEmployeeDialog(Employee employee) {
        JDialog dialog = new JDialog(this, "Personel Bilgileri", true);
        dialog.setSize(400, 600);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int gridy = 0;
        
        // id alanı (gizli)
        final String id;
        if (employee != null) {
            id = employee.getId();
        } else {
            // 1-100 arası rastgele id oluşturma
            String tempId = String.valueOf(1 + (int)(Math.random() * 100));
            // id zaten varsa yeni id oluşturma
            while (dataService.getEmployeeById(tempId) != null) {
                tempId = String.valueOf(1 + (int)(Math.random() * 100));
            }
            id = tempId;
        }

        // ad alanı
        gbc.gridx = 0;
        gbc.gridy = gridy++;
        panel.add(new JLabel("Ad:"), gbc);
        JTextField firstNameField = new JTextField(20);
        if (employee != null) firstNameField.setText(employee.getFirstName());
        gbc.gridx = 1;
        panel.add(firstNameField, gbc);

        // soyad alanı
        gbc.gridx = 0;
        gbc.gridy = gridy++;
        panel.add(new JLabel("Soyad:"), gbc);
        JTextField lastNameField = new JTextField(20);
        if (employee != null) lastNameField.setText(employee.getLastName());
        gbc.gridx = 1;
        panel.add(lastNameField, gbc);

        // tc no alanı
        gbc.gridx = 0;
        gbc.gridy = gridy++;
        panel.add(new JLabel("TC No:"), gbc);
        JTextField tcNoField = new JTextField(20);
        if (employee != null) tcNoField.setText(employee.getTcNo());
        gbc.gridx = 1;
        panel.add(tcNoField, gbc);

        // departman alanı
        gbc.gridx = 0;
        gbc.gridy = gridy++;
        panel.add(new JLabel("Departman:"), gbc);
        String[] departments = {
            "Yazılım Geliştirme",
            "Test ve Kalite",
            "Proje Yönetimi",
            "İş Analizi",
            "UI/UX Tasarım",
            "DevOps",
            "Sistem Yönetimi",
            "Teknik Destek",
            "İnsan Kaynakları",
            "Satış ve Pazarlama"
        };
        JComboBox<String> departmentComboBox = new JComboBox<>(departments);
        if (employee != null) departmentComboBox.setSelectedItem(employee.getDepartment());
        gbc.gridx = 1;
        panel.add(departmentComboBox, gbc);

        // pozisyon alanı
        gbc.gridx = 0;
        gbc.gridy = gridy++;
        panel.add(new JLabel("Pozisyon:"), gbc);
        JTextField positionField = new JTextField(20);
        if (employee != null) positionField.setText(employee.getPosition());
        gbc.gridx = 1;
        panel.add(positionField, gbc);

        // e-posta alanı
        gbc.gridx = 0;
        gbc.gridy = gridy++;
        panel.add(new JLabel("E-posta:"), gbc);
        JTextField emailField = new JTextField(20);
        if (employee != null) emailField.setText(employee.getEmail());
        gbc.gridx = 1;
        panel.add(emailField, gbc);

        // telefon alanı
        gbc.gridx = 0;
        gbc.gridy = gridy++;
        panel.add(new JLabel("Telefon:"), gbc);
        JTextField phoneField = new JTextField(20);
        if (employee != null) phoneField.setText(employee.getPhone());
        gbc.gridx = 1;
        panel.add(phoneField, gbc);

        // maaş alanı
        gbc.gridx = 0;
        gbc.gridy = gridy++;
        panel.add(new JLabel("Maaş:"), gbc);
        JTextField salaryField = new JTextField(20);
        if (employee != null) salaryField.setText(String.valueOf(employee.getSalary()));
        gbc.gridx = 1;
        panel.add(salaryField, gbc);

        // işe başlama tarihi alanı
        gbc.gridx = 0;
        gbc.gridy = gridy++;
        panel.add(new JLabel("İşe Başlama Tarihi:"), gbc);
        JDateChooser startDateChooser = new JDateChooser();
        startDateChooser.setDateFormatString("dd.MM.yyyy");
        if (employee != null && employee.getStartDate() != null) {
            startDateChooser.setDate(java.sql.Date.valueOf(employee.getStartDate()));
        }
        gbc.gridx = 1;
        panel.add(startDateChooser, gbc);

        // kullanıcı adı alanı
        gbc.gridx = 0;
        gbc.gridy = gridy++;
        panel.add(new JLabel("Kullanıcı Adı:"), gbc);
        JTextField usernameField = new JTextField(20);
        if (employee != null) usernameField.setText(employee.getUsername());
        gbc.gridx = 1;
        panel.add(usernameField, gbc);

        // şifre alanı
        gbc.gridx = 0;
        gbc.gridy = gridy++;
        panel.add(new JLabel("Şifre:"), gbc);
        JPasswordField passwordField = new JPasswordField(20);
        if (employee != null) passwordField.setText(employee.getPassword());
        gbc.gridx = 1;
        panel.add(passwordField, gbc);

        // butonlar
        JPanel buttonPanel = new JPanel();
        JButton saveButton = new JButton("Kaydet");
        JButton cancelButton = new JButton("İptal");

        saveButton.addActionListener(e -> {
            try {
                Employee newEmployee = new Employee();
                newEmployee.setId(id);
                newEmployee.setFirstName(firstNameField.getText());
                newEmployee.setLastName(lastNameField.getText());
                newEmployee.setTcNo(tcNoField.getText());
                newEmployee.setDepartment((String) departmentComboBox.getSelectedItem());
                newEmployee.setPosition(positionField.getText());
                newEmployee.setEmail(emailField.getText());
                newEmployee.setPhone(phoneField.getText());
                newEmployee.setSalary(Double.parseDouble(salaryField.getText()));
                newEmployee.setStartDate(startDateChooser.getDate().toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate());
                newEmployee.setUsername(usernameField.getText());
                newEmployee.setPassword(new String(passwordField.getPassword()));
                newEmployee.setRole(Employee.UserRole.EMPLOYEE);  // her zaman personel olarak ayarla

                if (employee == null) {
                    dataService.addEmployee(newEmployee);
                } else {
                    dataService.updateEmployee(newEmployee);
                }

                refreshEmployeeTable();
                dialog.dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog,
                    "Hata: Maaş alanına geçerli bir sayı giriniz!",
                    "Hata",
                    JOptionPane.ERROR_MESSAGE);
            } catch (DateTimeParseException ex) {
                JOptionPane.showMessageDialog(dialog,
                    "Hata: Geçersiz tarih formatı! Lütfen GG.AA.YYYY formatında girin.",
                    "Hata",
                    JOptionPane.ERROR_MESSAGE);
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(dialog,
                    "Hata: " + ex.getMessage(),
                    "Hata",
                    JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        gbc.gridx = 0;
        gbc.gridy = gridy;
        gbc.gridwidth = 2;
        panel.add(buttonPanel, gbc);

        dialog.add(new JScrollPane(panel));
        dialog.setVisible(true);
    }
} 