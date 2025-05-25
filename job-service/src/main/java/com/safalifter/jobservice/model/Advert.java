package com.safalifter.jobservice.model;

import com.safalifter.jobservice.enums.AdvertStatus;
import com.safalifter.jobservice.enums.Advertiser;
import lombok.*;

import javax.persistence.*;
import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Advert extends BaseEntity {
    private String userId;
    private String name;
    private String description;
    private int deliveryTime;
    private int price;
    private String imageId;

    @Enumerated(EnumType.STRING)
    private AdvertStatus status;

    @Enumerated(EnumType.STRING)
    private Advertiser advertiser;

    private Job job;
}
