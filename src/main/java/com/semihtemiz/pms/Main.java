package com.semihtemiz.pms;

import com.semihtemiz.pms.service.DataStorageService;
import com.semihtemiz.pms.ui.LoginFrame;

/**
 * Personel Yönetim Sistemi
 * Çalışanların bilgilerini yönetmek için kullanılan masaüstü uygulaması
 */
public class Main {
    /**
     * Uygulamayı başlatan ana metod
     */
    public static void main(String[] args) {
        DataStorageService dataService = new DataStorageService();
        LoginFrame loginFrame = new LoginFrame(dataService);
        loginFrame.setVisible(true);
    }
} 