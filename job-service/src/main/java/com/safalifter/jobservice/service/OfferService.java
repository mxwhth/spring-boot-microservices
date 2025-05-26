package com.safalifter.jobservice.service;

import com.safalifter.jobservice.client.UserServiceClient;
import com.safalifter.jobservice.dto.UserDto;
import com.safalifter.jobservice.enums.OfferStatus;
import com.safalifter.jobservice.exc.NotFoundException;
import com.safalifter.jobservice.model.Advert;
import com.safalifter.jobservice.model.Offer;
import com.safalifter.jobservice.po.AdvertPO;
import com.safalifter.jobservice.po.OfferPO;
import com.safalifter.jobservice.repository.AdvertRepository;
import com.safalifter.jobservice.repository.OfferRepository;
import com.safalifter.jobservice.request.notification.SendNotificationRequest;
import com.safalifter.jobservice.request.offer.MakeAnOfferRequest;
import com.safalifter.jobservice.request.offer.OfferUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.modelmapper.ModelMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OfferService {
    private final OfferRepository offerRepository;
    private final UserServiceClient userServiceclient;
    private final KafkaTemplate<String, SendNotificationRequest> kafkaTemplate;
    private final NewTopic topic;
    private final ModelMapper modelMapper;
    private final AdvertRepository advertRepository;

    @Transactional
    public Offer makeAnOffer(MakeAnOfferRequest request) {
        String userId = getUserById(request.getUserId()).getId();
        AdvertPO advertPO = advertRepository.findById(request.getAdvertId())
                .orElseThrow(() -> new NotFoundException("Advert not found"));
        Offer toSave = Offer.builder()
                .userId(userId)
                .advert(modelMapper.map(advertPO, Advert.class))
                .offeredPrice(request.getOfferedPrice())
                .status(OfferStatus.OPEN).build();
        saveOrUpdateCategory(toSave);

        SendNotificationRequest notification = SendNotificationRequest.builder()
                .message("You have received an offer for your advertising.")
                .userId(advertPO.getUserId())
                .offerId(toSave.getId()).build();

        kafkaTemplate.send(topic.name(), notification);
        return toSave;
    }

    public Offer getOfferById(String id) {
        return findOfferById(id);
    }

    public List<Offer> getOffersByAdvertId(String advertId) {
        Advert advert = advertRepository.findById(advertId)
                .map(po -> modelMapper.map(po, Advert.class))
                .orElseThrow(() -> new NotFoundException("Advert not found"));
        return offerRepository.getOffersByAdvertId(advert.getId()).stream()
                .map(o -> modelMapper.map(o, Offer.class))
                .peek(offer -> offer.setAdvert(advert))
                .collect(Collectors.toList());
    }

    public List<Offer> getOffersByUserId(String userId) {
        UserDto user = getUserById(userId);
        var offerPOList = offerRepository.getOffersByUserId(userId);
        var advertIds = offerPOList.stream().map(OfferPO::getAdvertId).distinct().toList();
        var advertMap = advertRepository.findAllById(advertIds).stream()
                .map(po -> modelMapper.map(po, Advert.class))
                .collect(Collectors.toMap(Advert::getId, Function.identity()));
        return offerPOList.stream().map(po -> {
            var offer = modelMapper.map(po, Offer.class);
            offer.setAdvert(advertMap.get(po.getAdvertId()));
            return offer;
        }).toList();
    }

    public UserDto getUserById(String id) {
        return Optional.ofNullable(userServiceclient.getUserById(id).getBody())
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    @Transactional
    public Offer updateOfferById(OfferUpdateRequest request) {
        Offer toUpdate = findOfferById(request.getId());
        modelMapper.map(request, toUpdate);
        return saveOrUpdateCategory(toUpdate);
    }

    @Transactional
    public void deleteOfferById(String id) {
        offerRepository.deleteById(id);
    }

    public boolean authorizeCheck(String id, String principal) {
        return getUserById(getOfferById(id).getUserId()).getUsername().equals(principal);
    }

    protected Offer findOfferById(String id) {
        return offerRepository.findById(id)
                .map(po -> modelMapper.map(po, Offer.class))
                .orElseThrow(() -> new NotFoundException("Offer not found"));
    }

    private Offer saveOrUpdateCategory(Offer offer) {
        OfferPO savedOffer = offerRepository.save(modelMapper.map(offer, OfferPO.class));
        return modelMapper.map(savedOffer, Offer.class);
    }
}
