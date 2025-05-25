package com.safalifter.jobservice.po;

import com.safalifter.jobservice.enums.AdvertStatus;
import com.safalifter.jobservice.enums.Advertiser;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import java.util.List;

@Entity(name = "adverts")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AdvertPO extends BaseEntity {
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

    private String jobId;
}
