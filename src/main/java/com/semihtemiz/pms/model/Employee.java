package com.semihtemiz.pms.model;

import java.io.Serializable;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 *personel bilgilerini temsil eden sınıf
 *
 *kişisel bilgiler (id, ad, soyad, tc no)
 *iş bilgileri (departman, pozisyon, maaş)
 *iletişim bilgileri (e-posta, telefon)
 *sistem bilgileri (kullanıcı adı, şifre, rol)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Employee implements Serializable {
    private static final long serialVersionUID = 1L;

    //kişisel bilgiler
    private String id;
    private String firstName;
    private String lastName;
    private String tcNo;
    
    //iş bilgileri
    private String department;
    private String position;
    private String email;
    private String phone;
    private double salary;
    private double hourlyRate;
    private LocalDate startDate;
    private int vacationDays;
    
    //sistem bilgileri
    private String username;
    private String password;
    private UserRole role;

    /**
     *kullanıcı rolleri
     *
     *admin: sistem yöneticisi
     *employee: normal personel
     */
    public enum UserRole {
        //yönetici rolü
        ADMIN("Yönetici"),
        
        //personel rolü
        EMPLOYEE("Personel");

        private final String displayName;

        UserRole(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    /**
     *30 gün izin hakkı
     */
    public Employee() {
        this.vacationDays = 30;
    }

    /**
     *tüm bilgileri içeren yapı
     *
     * @param id benzersiz id
     * @param firstName ad
     * @param lastName soyad
     * @param tcNo tc kimlik no
     * @param department departman
     * @param position pozisyon
     * @param email e-posta
     * @param phone telefon
     * @param salary maaş
     * @param startDate işe başlama tarihi
     * @param username kullanıcı adı
     * @param password şifre
     * @param role kullanıcı rolü
     */
    public Employee(String id, String firstName, String lastName, String tcNo,
                   String department, String position, String email, String phone,
                   double salary, LocalDate startDate, String username, String password, UserRole role) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.tcNo = tcNo;
        this.department = department;
        this.position = position;
        this.email = email;
        this.phone = phone;
        this.salary = salary;
        this.hourlyRate = calculateHourlyRate(salary);
        this.startDate = startDate;
        this.vacationDays = 30;
        this.username = username;
        this.password = password;
        this.role = role;
    }

    /**
     *saatlik ücreti hesaplama
     *
     *aylık maaş / (21 gün * 8 saat)
     *
     * @param monthlySalary aylık maaş
     * @return saatlik ücret
     */
    private double calculateHourlyRate(double monthlySalary) {
        return monthlySalary / (21 * 8);
    }

    /**
     *eski format tam ad alanını işleme
     *
     * @param fullName tam ad
     */
    @JsonSetter("fullName")
    public void setFullNameFromJson(String fullName) {
        if (fullName != null && !fullName.isEmpty()) {
            String[] parts = fullName.split(" ", 2);
            this.firstName = parts[0];
            this.lastName = parts.length > 1 ? parts[1] : "";
        }
    }

    // getter ve setter metodları
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public String getTcNo() {
        return tcNo;
    }

    public void setTcNo(String tcNo) {
        this.tcNo = tcNo;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public double getSalary() {
        return salary;
    }

    public void setSalary(double salary) {
        this.salary = salary;
        this.hourlyRate = calculateHourlyRate(salary);
    }

    public double getHourlyRate() {
        return hourlyRate;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public int getVacationDays() {
        return vacationDays;
    }

    public void setVacationDays(int vacationDays) {
        this.vacationDays = vacationDays;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    @Override
    public String toString() {
        return "Employee{" +
                "id='" + id + '\'' +
                ", name='" + getFullName() + '\'' +
                ", department='" + department + '\'' +
                ", position='" + position + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
} 