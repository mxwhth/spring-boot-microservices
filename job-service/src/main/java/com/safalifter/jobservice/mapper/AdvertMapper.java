package com.safalifter.jobservice.mapper;

import com.safalifter.jobservice.enums.Advertiser;
import com.safalifter.jobservice.model.Advert;
import com.safalifter.jobservice.po.AdvertPO;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
public interface AdvertMapper {
    List<AdvertPO> getAdvertsByUserIdAndAdvertiser(String userId, Advertiser advertiser);

    AdvertPO findById(String id);

    AdvertPO save(AdvertPO advertPO);

    AdvertPO update(AdvertPO advertPO);

    void deleteById(String id);

    List<AdvertPO> findAll();
}
