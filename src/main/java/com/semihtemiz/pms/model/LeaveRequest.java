package com.semihtemiz.pms.model;

import java.io.Serializable;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 *izin taleplerini temsil eden sınıf
 *
 *izin tarihleri başlangıç/bitiş
 *izin nedeni
 *onay durumu
 *yönetici notları
 */
public class LeaveRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    
    //id
    private String id;
    
    //personel bilgileri
    private String employeeId;
    
    //izin tarihleri
    private LocalDate startDate;
    private LocalDate endDate;
    
    //izin detayları
    private String reason;
    private LeaveStatus status;
    private String approverNotes;

    /**
     *izin durumları
     *
     *pending: beklemede
     *approved: onaylandı
     *rejected: reddedildi
     */
    public enum LeaveStatus {
        //bekleyen talep
        PENDING("Beklemede"),
        
        //onaylanan talep
        APPROVED("Onaylandı"),
        
        //reddedilen talep
        REJECTED("Reddedildi");

        private final String displayName;

        LeaveStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public LeaveRequest() {
    }

    /**
     *yeni izin talebi oluşturma
     *
     *durum otomatik olarak beklemede
     *
     * @param id benzersiz id
     * @param employeeId personel id
     * @param startDate başlangıç tarihi
     * @param endDate bitiş tarihi
     * @param reason izin nedeni
     */
    public LeaveRequest(String id, String employeeId, LocalDate startDate, 
                       LocalDate endDate, String reason) {
        this.id = id;
        this.employeeId = employeeId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.reason = reason;
        this.status = LeaveStatus.PENDING;
    }

    /**
     *izin süresini hesaplama
     *
     *başlangıç ve bitiş dahil
     *
     * @return toplam gün sayısı
     */
    @JsonIgnore
    public int getDurationInDays() {
        return (int) startDate.until(endDate.plusDays(1)).getDays();
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

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LeaveStatus getStatus() {
        return status;
    }

    public void setStatus(LeaveStatus status) {
        this.status = status;
    }

    public String getApproverNotes() {
        return approverNotes;
    }

    public void setApproverNotes(String approverNotes) {
        this.approverNotes = approverNotes;
    }
} 