package com.woori.wonnie.subscription;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "service") @Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ServiceCatalog {
    @Id private String id;
    private String brandColor;
    private String initialText;
}
