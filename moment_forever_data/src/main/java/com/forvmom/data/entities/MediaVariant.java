package com.forvmom.data.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.util.Date;

@Entity
@Table(name = "media_variants", uniqueConstraints = {
        @UniqueConstraint(name = "uk_media_variant_media_type", columnNames = { "media_id", "variant_type" })
})
public class MediaVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_id", nullable = false)
    private Media media;

    @Enumerated(EnumType.STRING)
    @Column(name = "variant_type", nullable = false, length = 20)
    private MediaVariantType variantType;

    @Column(name = "storage_file_name", nullable = false, length = 500)
    private String storageFileName;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "width_px")
    private Integer widthPx;

    @Column(name = "height_px")
    private Integer heightPx;

    @CreationTimestamp
    @Column(name = "created_on", updatable = false)
    private Date createdOn;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Media getMedia() {
        return media;
    }

    public void setMedia(Media media) {
        this.media = media;
    }

    public MediaVariantType getVariantType() {
        return variantType;
    }

    public void setVariantType(MediaVariantType variantType) {
        this.variantType = variantType;
    }

    public String getStorageFileName() {
        return storageFileName;
    }

    public void setStorageFileName(String storageFileName) {
        this.storageFileName = storageFileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(Long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public Integer getWidthPx() {
        return widthPx;
    }

    public void setWidthPx(Integer widthPx) {
        this.widthPx = widthPx;
    }

    public Integer getHeightPx() {
        return heightPx;
    }

    public void setHeightPx(Integer heightPx) {
        this.heightPx = heightPx;
    }

    public Date getCreatedOn() {
        return createdOn;
    }
}

