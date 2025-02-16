package com.semihtemiz.pms.model;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 *giriş/çıkış kayıtlarını temsil eden sınıf
 *
 *giriş/çıkış zamanları
 *çalışma süresi
 *fazla mesai durumu
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TimeEntry implements Serializable {
    private static final long serialVersionUID = 1L;
    
    //id
    private String id;
    
    //personel id
    private String employeeId;
    
    //zaman bilgileri
    private LocalDateTime checkIn;
    private LocalDateTime checkOut;
    
    //süre bilgileri
    private Duration workDuration;
    private boolean overtime;

    
    public TimeEntry() {
    }

    /**
     *giriş kaydı oluşturma
     *
     *sadece giriş zamanı kaydedilir
     *çıkış zamanı sonradan set edilir
     *
     * @param id benzersiz id
     * @param employeeId personel id
     * @param checkIn giriş zamanı
     */
    public TimeEntry(String id, String employeeId, LocalDateTime checkIn) {
        this.id = id;
        this.employeeId = employeeId;
        this.checkIn = checkIn;
    }

    /**
     *çalışma süresini hesaplama
     *
     *süre = çıkış - giriş zamanı
     *fazla mesai = süre > 8 saat
     */
    public void calculateWorkDuration() {
        if (checkIn != null && checkOut != null) {
            workDuration = Duration.between(checkIn, checkOut);
            //8 saatten fazla çalışma mesai sayılır
            overtime = workDuration.toHours() > 8;
        }
    }

    // getter ve setter metodları
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public LocalDateTime getCheckIn() {
        return checkIn;
    }

    public void setCheckIn(LocalDateTime checkIn) {
        this.checkIn = checkIn;
    }

    public LocalDateTime getCheckOut() {
        return checkOut;
    }

    public void setCheckOut(LocalDateTime checkOut) {
        this.checkOut = checkOut;
        calculateWorkDuration();
    }

    public Duration getWorkDuration() {
        return workDuration;
    }

    public boolean isOvertime() {
        return overtime;
    }

    public void setOvertime(boolean overtime) {
        this.overtime = overtime;
    }

    @Override
    public String toString() {
        return "TimeEntry{" +
                "id='" + id + '\'' +
                ", employeeId='" + employeeId + '\'' +
                ", checkIn=" + checkIn +
                ", checkOut=" + checkOut +
                ", workDuration=" + workDuration +
                ", overtime=" + overtime +
                '}';
    }
} 