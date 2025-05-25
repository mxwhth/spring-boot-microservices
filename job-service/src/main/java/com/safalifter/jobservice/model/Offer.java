package com.safalifter.jobservice.model;

import com.safalifter.jobservice.enums.OfferStatus;
import lombok.*;

import javax.persistence.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Offer extends BaseEntity {
    private String userId;
    private int offeredPrice;

    @Enumerated(EnumType.STRING)
    private OfferStatus status;

    private Advert advert;
}
