package com.safalifter.jobservice.service;

import com.safalifter.jobservice.client.UserServiceClient;
import com.safalifter.jobservice.dto.UserDto;
import com.safalifter.jobservice.enums.OfferStatus;
import com.safalifter.jobservice.exc.NotFoundException;
import com.safalifter.jobservice.model.Advert;
import com.safalifter.jobservice.model.Offer;
import com.safalifter.jobservice.repository.OfferRepository;
import com.safalifter.jobservice.request.notification.SendNotificationRequest;
import com.safalifter.jobservice.request.offer.MakeAnOfferRequest;
import com.safalifter.jobservice.request.offer.OfferUpdateRequest;
import com.safalifter.jobservice.transaction.ClearCacheAfterTransactionEvent;
import com.safalifter.jobservice.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OfferService {
    private final OfferRepository offerRepository;
    private final AdvertService advertService;
    private final UserServiceClient userServiceclient;
    private final KafkaTemplate<String, SendNotificationRequest> kafkaTemplate;
    private final NewTopic topic;
    private final ModelMapper modelMapper;
    private final RedisUtil redisUtil;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Offer makeAnOffer(MakeAnOfferRequest request) {
        String userId = getUserById(request.getUserId()).getId();
        Advert advert = advertService.getAdvertById(request.getAdvertId());
        Offer toSave = Offer.builder()
                .userId(userId)
                .advert(advert)
                .offeredPrice(request.getOfferedPrice())
                .status(OfferStatus.OPEN).build();
        saveOrUpdateCategory(toSave);

        SendNotificationRequest notification = SendNotificationRequest.builder()
                .message("You have received an offer for your advertising.")
                .userId(advert.getUserId())
                .offerId(toSave.getId()).build();

        kafkaTemplate.send(topic.name(), notification);
        return toSave;
    }

    public Offer getOfferById(String id) {
        return findOfferById(id);
    }

    public List<Offer> getOffersByAdvertId(String id) {
        Advert advert = advertService.getAdvertById(id);
        return offerRepository.getOffersByAdvertId(advert.getId());
    }

    public List<Offer> getOffersByUserId(String id) {
        String userId = getUserById(id).getId();
        return offerRepository.getOffersByUserId(userId);
    }

    public UserDto getUserById(String id) {
        return Optional.ofNullable(userServiceclient.getUserById(id).getBody())
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    @Transactional
    public Offer updateOfferById(OfferUpdateRequest request) {
        redisUtil.delete(getOfferCacheId(request.getId()));
        Offer toUpdate = findOfferById(request.getId(), false);
        modelMapper.map(request, toUpdate);
        return saveOrUpdateCategory(toUpdate);
    }

    @Transactional
    public void deleteOfferById(String id) {
        offerRepository.deleteById(id);
        eventPublisher.publishEvent(new ClearCacheAfterTransactionEvent(getOfferCacheId(id)));
    }

    public boolean authorizeCheck(String id, String principal) {
        return getUserById(getOfferById(id).getUserId()).getUsername().equals(principal);
    }

    protected Offer findOfferById(String id) {
        return findOfferById(id, true);
    }

    protected Offer findOfferById(String id, boolean useCache) {
        if (useCache) {
            Offer offerCache = redisUtil.findObject(getOfferCacheId(id), Offer.class);
            if (offerCache != null) {
                return offerCache;
            }
        }
        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Offer not found"));
        redisUtil.saveObject(getOfferCacheId(id), offer);
        return offer;
    }

    private Offer saveOrUpdateCategory(Offer offer) {
        Offer savedOffer = offerRepository.save(offer);
//        redisUtil.saveObject(getOfferCacheId(savedOffer.getId()), savedOffer);
        eventPublisher.publishEvent(new ClearCacheAfterTransactionEvent(getOfferCacheId(savedOffer.getId())));
        return savedOffer;
    }

    private String getOfferCacheId(String id) {
        return "offer:" + id;
    }
}
