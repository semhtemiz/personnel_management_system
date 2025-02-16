/**
 *veri depolama servisi
 *
 *json formatında veri saklama:
 *personel bilgileri (employees.json)
 *giriş/çıkış kayıtları (time_entries.json)
 *izin talepleri (leave_requests.json)
 *
 *özellikler:
 *thread-safe liste yapıları
 *otomatik dosya yedekleme
 *utf-8 karakter desteği
 */
package com.semihtemiz.pms.service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.semihtemiz.pms.model.Employee;
import com.semihtemiz.pms.model.LeaveRequest;
import com.semihtemiz.pms.model.TimeEntry;

public class DataStorageService {
    //veri dosyalarının konumu
    private static final String DATA_DIR = "data";
    
    //json dosya yolları
    private static final String EMPLOYEES_FILE = DATA_DIR + "/employees.json";
    private static final String TIME_ENTRIES_FILE = DATA_DIR + "/time_entries.json";
    private static final String LEAVE_REQUESTS_FILE = DATA_DIR + "/leave_requests.json";
    
    //json dönüşüm nesnesi
    private final ObjectMapper objectMapper;
    
    //thread-safe veri listeleri
    private List<Employee> employees;
    private List<TimeEntry> timeEntries;
    private List<LeaveRequest> leaveRequests;

    /**
     *servis başlatma
     *
     *json dönüştürücü ayarlama
     *thread-safe liste oluşturma
     *veri dizini hazırlama
     *mevcut verileri yükleme
     */
    public DataStorageService() {
        //json dönüştürücü ayarları
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.configure(com.fasterxml.jackson.core.json.JsonWriteFeature.ESCAPE_NON_ASCII.mappedFeature(), false);
        
        //thread-safe liste başlatma
        employees = new CopyOnWriteArrayList<>();
        timeEntries = new CopyOnWriteArrayList<>();
        leaveRequests = new CopyOnWriteArrayList<>();
        
        //veri dizini ve dosya hazırlığı
        initializeDataDirectory();
        initializeData();
    }

    /**
     *veri dizini oluşturma
     *
     *data dizini oluşturma
     *json dosyaları oluşturma
     *varsayılan admin oluşturma
     */
    private void initializeDataDirectory() {
        File dataDir = new File(DATA_DIR);
        if (!dataDir.exists()) {
            if (!dataDir.mkdirs()) {
                System.err.println("veri dizini oluşturulamadı: " + DATA_DIR);
            }
        }
        
        try {
            //utf-8 formatında boş dosyaları oluştur
            File employeesFile = new File(EMPLOYEES_FILE);
            File timeEntriesFile = new File(TIME_ENTRIES_FILE);
            File leaveRequestsFile = new File(LEAVE_REQUESTS_FILE);

            if (!employeesFile.exists()) {
                //varsayılan admin kullanıcısı
                List<Employee> defaultEmployees = new ArrayList<>();
                Employee admin = new Employee();
                admin.setId("0");
                admin.setUsername("admin");
                admin.setPassword("admin");
                admin.setFirstName("System");
                admin.setLastName("Admin");
                admin.setRole(Employee.UserRole.ADMIN);
                admin.setDepartment("Yonetim");
                admin.setPosition("Sistem Yoneticisi");
                admin.setEmail("admin@company.com");
                admin.setPhone("0000000000");
                admin.setSalary(0.0);
                admin.setStartDate(LocalDate.now());
                admin.setVacationDays(0);
                defaultEmployees.add(admin);
                
                objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
                objectMapper.writeValue(employeesFile, defaultEmployees);
            }

            if (!timeEntriesFile.exists()) {
                objectMapper.writeValue(timeEntriesFile, new ArrayList<>());
            }

            if (!leaveRequestsFile.exists()) {
                objectMapper.writeValue(leaveRequestsFile, new ArrayList<>());
            }
        } catch (IOException e) {
            System.err.println("veri dosyaları oluşturulamadı: " + e.getMessage());
        }
    }

    /**
     *verileri dosyadan yükleme
     *
     *json dosyalarını okuma
     *listelere aktarma
     *hata durumunda boş liste oluşturma
     */
    private void initializeData() {
        try {
            File employeesFile = new File(EMPLOYEES_FILE);
            File timeEntriesFile = new File(TIME_ENTRIES_FILE);
            File leaveRequestsFile = new File(LEAVE_REQUESTS_FILE);

            if (employeesFile.exists() && employeesFile.length() > 0) {
                employees = new ArrayList<>(List.of(objectMapper.readValue(employeesFile, Employee[].class)));
            }

            if (timeEntriesFile.exists() && timeEntriesFile.length() > 0) {
                timeEntries = new ArrayList<>(List.of(objectMapper.readValue(timeEntriesFile, TimeEntry[].class)));
            }

            if (leaveRequestsFile.exists() && leaveRequestsFile.length() > 0) {
                leaveRequests = new ArrayList<>(List.of(objectMapper.readValue(leaveRequestsFile, LeaveRequest[].class)));
            }
        } catch (IOException e) {
            System.err.println("veri yüklenirken hata oluştu: " + e.getMessage());
            //hata durumunda boş liste oluştur
            employees = new ArrayList<>();
            timeEntries = new ArrayList<>();
            leaveRequests = new ArrayList<>();
        }
    }

    /**
     *verileri dosyaya kaydetme
     *
     *dizin kontrolü
     *json formatında yazma
     *hata durumunda log kaydetme
     */
    public void saveData() {
        try {
            //dizin kontrolü
            new File(DATA_DIR).mkdirs();
            
            //json formatında kaydet
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
            objectMapper.writeValue(new File(EMPLOYEES_FILE), employees);
            objectMapper.writeValue(new File(TIME_ENTRIES_FILE), timeEntries);
            objectMapper.writeValue(new File(LEAVE_REQUESTS_FILE), leaveRequests);
        } catch (IOException e) {
            System.err.println("veri kaydedilirken hata oluştu: " + e.getMessage());
        }
    }

    /**
     *yeni personel ekleme
     *
     * @param employee eklenecek personel
     */
    public void addEmployee(Employee employee) {
        employees.add(employee);
        saveData();
    }

    /**
     *personel bilgilerini güncelleme
     *
     * @param employee güncellenecek personel (id değişmez)
     */
    public void updateEmployee(Employee employee) {
        for (int i = 0; i < employees.size(); i++) {
            if (employees.get(i).getId().equals(employee.getId())) {
                employees.set(i, employee);
                break;
            }
        }
        saveData();
    }

    /**
     *personel ve ilişkili verileri silme
     *
     *personel bilgileri
     *giriş/çıkış kayıtları
     *izin talepleri
     *
     * @param employeeId silinecek personel id
     */
    public void deleteEmployee(String employeeId) {
        //personeli sil
        employees.removeIf(e -> e.getId().equals(employeeId));
        
        //giriş/çıkış kayıtlarını sil
        timeEntries.removeIf(entry -> entry.getEmployeeId().equals(employeeId));
        
        //izin taleplerini sil
        leaveRequests.removeIf(request -> request.getEmployeeId().equals(employeeId));
        
        //değişiklikleri kaydet
        saveData();
    }

    /**
     *tüm personel listesini getirme
     *
     * @return personel listesi
     */
    public List<Employee> getAllEmployees() {
        return new ArrayList<>(employees);
    }

    /**
     *id ile personel bulma
     *
     * @param id personel id
     * @return bulunan personel veya null
     */
    public Employee getEmployeeById(String id) {
        return employees.stream()
                .filter(e -> e.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    /**
     *kullanıcı adı ile personel bulma
     *
     * @param username kullanıcı adı
     * @return bulunan personel veya null
     */
    public Employee getEmployeeByUsername(String username) {
        return employees.stream()
                .filter(e -> e.getUsername().equals(username))
                .findFirst()
                .orElse(null);
    }

    
    /**
     *yeni giriş/çıkış kaydı ekleme
     *
     * @param timeEntry eklenecek kayıt
     */
    public void addTimeEntry(TimeEntry timeEntry) {
        timeEntries.add(timeEntry);
        saveData();
    }

    /**
     *giriş/çıkış kaydını güncelleme
     *
     * @param timeEntry güncellenecek kayıt
     */
    public void updateTimeEntry(TimeEntry timeEntry) {
        for (int i = 0; i < timeEntries.size(); i++) {
            if (timeEntries.get(i).getId().equals(timeEntry.getId())) {
                timeEntries.set(i, timeEntry);
                break;
            }
        }
        saveData();
    }

    /**
     *personelin giriş/çıkış kayıtlarını getirme
     *
     * @param employeeId personel id
     * @return giriş/çıkış kayıtları listesi
     */
    public List<TimeEntry> getTimeEntriesByEmployeeId(String employeeId) {
        List<TimeEntry> employeeEntries = new ArrayList<>();
        for (TimeEntry entry : timeEntries) {
            if (entry.getEmployeeId().equals(employeeId)) {
                employeeEntries.add(entry);
            }
        }
        return employeeEntries;
    }

    /**
     *tüm giriş/çıkış kayıtlarını getirme
     *
     * @return giriş/çıkış kayıtları listesi
     */
    public List<TimeEntry> getAllTimeEntries() {
        return new ArrayList<>(timeEntries);
    }

    /**
     *tüm giriş/çıkış kayıtlarını silme
     */
    public void clearTimeEntries() {
        timeEntries.clear();
        saveData();
    }
    
    /**
     *yeni izin talebi ekleme
     *
     * @param request eklenecek talep
     */
    public void addLeaveRequest(LeaveRequest request) {
        leaveRequests.add(request);
        saveData();
    }

    /**
     *izin talebini güncelleme
     *
     * @param request güncellenecek talep
     */
    public void updateLeaveRequest(LeaveRequest request) {
        for (int i = 0; i < leaveRequests.size(); i++) {
            if (leaveRequests.get(i).getId().equals(request.getId())) {
                leaveRequests.set(i, request);
                break;
            }
        }
        saveData();
    }

    /**
     *personelin izin taleplerini getirme
     *
     * @param employeeId personel id
     * @return izin talepleri listesi
     */
    public List<LeaveRequest> getLeaveRequestsByEmployeeId(String employeeId) {
        return leaveRequests.stream()
                .filter(r -> r.getEmployeeId().equals(employeeId))
                .collect(Collectors.toList());
    }

    /**
     *bekleyen izin taleplerini getirme
     *
     * @return bekleyen izin talepleri listesi
     */
    public List<LeaveRequest> getPendingLeaveRequests() {
        return leaveRequests.stream()
                .filter(r -> r.getStatus() == LeaveRequest.LeaveStatus.PENDING)
                .collect(Collectors.toList());
    }

    /**
     *tüm izin taleplerini getirme
     *
     * @return izin talepleri listesi
     */
    public List<LeaveRequest> getAllLeaveRequests() {
        return new ArrayList<>(leaveRequests);
    }

    /**
     *kullanılan izin günlerini hesaplama
     *
     * @param employeeId personel id
     * @param year hesaplanacak yıl
     * @return kullanılan izin günü sayısı
     */
    public int getUsedLeaveDays(String employeeId, int year) {
        return leaveRequests.stream()
                .filter(r -> r.getEmployeeId().equals(employeeId) &&
                        r.getStatus() == LeaveRequest.LeaveStatus.APPROVED &&
                        r.getStartDate().getYear() == year)
                .mapToInt(LeaveRequest::getDurationInDays)
                .sum();
    }

    /**
     *tarih aralığındaki izin taleplerini getirme
     *
     * @param startDate başlangıç tarihi
     * @param endDate bitiş tarihi
     * @return izin talepleri listesi
     */
    public List<LeaveRequest> getLeaveRequestsByDateRange(LocalDate startDate, LocalDate endDate) {
        return leaveRequests.stream()
                .filter(r -> !r.getStartDate().isAfter(endDate) && !r.getEndDate().isBefore(startDate))
                .collect(Collectors.toList());
    }
} 