package com.safalifter.jobservice.mapper;

import com.safalifter.jobservice.enums.Advertiser;
import com.safalifter.jobservice.model.Advert;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface AdvertMapper {
    List<Advert> getAdvertsByUserIdAndAdvertiser(String id, Advertiser advertiser);
}
