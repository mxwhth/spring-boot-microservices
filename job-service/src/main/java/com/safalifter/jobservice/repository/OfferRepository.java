package com.safalifter.jobservice.repository;

import com.safalifter.jobservice.model.Offer;
import com.safalifter.jobservice.po.OfferPO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OfferRepository extends JpaRepository<OfferPO, String> {
    List<OfferPO> getOffersByUserId(String id);

    List<OfferPO> getOffersByAdvertId(String id);
}
