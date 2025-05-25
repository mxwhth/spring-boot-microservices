package com.safalifter.jobservice.po;

import com.safalifter.jobservice.enums.OfferStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity(name = "offers")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class OfferPO extends BaseEntity {
    private String userId;
    private int offeredPrice;

    @Enumerated(EnumType.STRING)
    private OfferStatus status;

    private String advertId;
}
