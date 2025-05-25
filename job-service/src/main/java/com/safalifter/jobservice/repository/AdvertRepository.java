package com.safalifter.jobservice.repository;

import com.safalifter.jobservice.enums.Advertiser;
import com.safalifter.jobservice.model.Advert;
import com.safalifter.jobservice.po.AdvertPO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdvertRepository extends JpaRepository<AdvertPO, String> {
    List<AdvertPO> getAdvertsByUserIdAndAdvertiser(String id, Advertiser advertiser);
}
