package com.luigimonteforte.conservationrequests.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;

    private String mimeType;

    private Long fileSize;
    private String hash;

    private Instant documentDate;

    @ManyToOne
    private Request request;
}
