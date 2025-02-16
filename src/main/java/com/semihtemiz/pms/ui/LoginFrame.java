package com.semihtemiz.pms.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.semihtemiz.pms.model.Employee;
import com.semihtemiz.pms.service.DataStorageService;

/**
 *giriş ekranı
 *
 *kullanıcı adı ve şifre doğrulama
 *başarılı girişte ana ekrana yönlendirme
 *başarısız girişte hata mesajı gösterme
 */
public class LoginFrame extends JFrame {
    //veri depolama servisi
    private final DataStorageService dataService;
    
    //giriş alanları
    private final JTextField usernameField;
    private final JPasswordField passwordField;

    /**
     *giriş ekranını oluşturma
     *
     * @param dataService veri depolama servisi
     */
    public LoginFrame(DataStorageService dataService) {
        this.dataService = dataService;

        //pencere özelliklerini ayarlama
        setTitle("Personel Yönetim Sistemi - Giriş");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 200);
        setLocationRelativeTo(null);
        setResizable(false);  //pencere boyutu değiştirilemez

        //ana panel ve grid düzeni
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);  

        //kullanıcı adı alanı
        gbc.gridx = 0;
        gbc.gridy = 0;
        mainPanel.add(new JLabel("Kullanıcı Adı:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        usernameField = new JTextField(20);
        mainPanel.add(usernameField, gbc);

        //şifre alanı
        gbc.gridx = 0;
        gbc.gridy = 1;
        mainPanel.add(new JLabel("Şifre:"), gbc);

        gbc.gridx = 1;
        passwordField = new JPasswordField(20);
        //enter tuşu ile giriş yapma
        passwordField.addActionListener(e -> handleLogin());
        mainPanel.add(passwordField, gbc);

        //giriş butonu
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        JButton loginButton = new JButton("Giriş");
        loginButton.addActionListener(e -> handleLogin());
        mainPanel.add(loginButton, gbc);

        //kullanıcı adı alanında enter a basınca şifre alanına geçme
        usernameField.addActionListener(e -> passwordField.requestFocusInWindow());

        add(mainPanel);

        //ilk çalıştırmada varsayılan admin kullanıcısını oluşturma
        if (dataService.getAllEmployees().isEmpty()) {
            createDefaultAdminUser();
        }
    }

    /**
     *varsayılan admin kullanıcısını oluşturma
     *
     *kullanıcı adı: admin
     *şifre: admin
     *rol: yönetici
     */
    private void createDefaultAdminUser() {
        Employee admin = new Employee();
        admin.setId("1");
        admin.setUsername("admin");
        admin.setPassword("admin");
        admin.setFirstName("System");
        admin.setLastName("Admin");
        admin.setRole(Employee.UserRole.ADMIN);
        admin.setDepartment("Yönetim");
        admin.setPosition("Sistem Yöneticisi");
        admin.setEmail("admin@company.com");
        admin.setPhone("0000000000");
        admin.setSalary(0.0);
        admin.setStartDate(LocalDate.now());
        admin.setVacationDays(0);
        dataService.addEmployee(admin);
    }

    /**
     *giriş işlemini gerçekleştirme
     *
     *kullanıcı adı ve şifre kontrolü
     *başarılı girişte ana ekranı açma
     *başarısız girişte hata mesajı gösterme
     */
    private void handleLogin() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());

        Employee employee = dataService.getEmployeeByUsername(username);

        if (employee != null && employee.getPassword().equals(password)) {
            //başarılı giriş - ana ekranı aç
            MainFrame mainFrame = new MainFrame(dataService, employee);
            mainFrame.setVisible(true);
            dispose();  
        } else {
            //başarısız giriş - hata mesajı göster
            JOptionPane.showMessageDialog(this,
                    "Geçersiz kullanıcı adı veya şifre!",
                    "Hata",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     *uygulamayı başlatma
     *
     *swing arayüzünde çalıştırma
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            DataStorageService dataService = new DataStorageService();
            LoginFrame loginFrame = new LoginFrame(dataService);
            loginFrame.setVisible(true);
        });
    }
} 