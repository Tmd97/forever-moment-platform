package com.forvmom.data.entities;

import com.forvmom.common.enums.VendorStatus;
import com.forvmom.data.entities.auth.AuthUser;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Table(name = "vendors")
@SQLDelete(sql = "UPDATE vendors SET deleted = true WHERE id = ?")
@Where(clause = "deleted = false")
@Entity
public class Vendor extends NamedEntity {

    @Column(name = "vendor_code", unique = true, length = 50)
    private String vendorCode;

    @Column(name = "business_name", nullable = false, length = 150)
    private String businessName;

    @Column(name = "category", nullable = false, length = 100)
    private String category;

    @Column(name = "contact_name", nullable = false, length = 100)
    private String contactName;

    @Column(name = "contact_email", nullable = false, unique = true, length = 150)
    private String contactEmail;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private VendorStatus status = VendorStatus.PENDING;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "auth_user_id", unique = true, nullable = false)
    private AuthUser authUser;

    public Vendor() {
    }

    public Vendor(String businessName, String category, String contactName, String contactEmail) {
        this.businessName = businessName;
        this.category = category;
        this.contactName = contactName;
        this.contactEmail = contactEmail;
        setName(businessName);
    }

    public String getVendorCode() {
        return vendorCode;
    }

    public void setVendorCode(String vendorCode) {
        this.vendorCode = vendorCode;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
        setName(businessName);
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public VendorStatus getStatus() {
        return status;
    }

    public void setStatus(VendorStatus status) {
        this.status = status;
    }

    public AuthUser getAuthUser() {
        return authUser;
    }

    public void setAuthUser(AuthUser authUser) {
        this.authUser = authUser;
    }
}