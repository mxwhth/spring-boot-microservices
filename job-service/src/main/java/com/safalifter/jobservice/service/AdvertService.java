package com.safalifter.jobservice.service;

import com.safalifter.jobservice.client.FileStorageClient;
import com.safalifter.jobservice.client.UserServiceClient;
import com.safalifter.jobservice.dto.UserDto;
import com.safalifter.jobservice.enums.AdvertStatus;
import com.safalifter.jobservice.enums.Advertiser;
import com.safalifter.jobservice.exc.NotFoundException;
import com.safalifter.jobservice.model.Advert;
import com.safalifter.jobservice.model.Job;
import com.safalifter.jobservice.po.AdvertPO;
import com.safalifter.jobservice.repository.AdvertRepository;
import com.safalifter.jobservice.repository.JobRepository;
import com.safalifter.jobservice.request.advert.AdvertCreateRequest;
import com.safalifter.jobservice.request.advert.AdvertUpdateRequest;
import com.safalifter.jobservice.transaction.ClearCacheAfterTransactionEvent;
import com.safalifter.jobservice.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdvertService {
    private final AdvertRepository advertRepository;
    private final UserServiceClient userServiceclient;
    private final FileStorageClient fileStorageClient;
    private final ModelMapper modelMapper;
    private final RedisUtil redisUtil;
    private final ApplicationEventPublisher eventPublisher;
    private final JobRepository jobRepository;

    @Transactional
    public Advert createAdvert(AdvertCreateRequest request, MultipartFile file) {
        String userId = getUserById(request.getUserId()).getId();
        Job job = jobRepository.findById(request.getJobId())
                .map(po -> modelMapper.map(po, Job.class))
                .orElseThrow(() -> new NotFoundException("Job not found"));

        String imageId = null;

        if (file != null) {
            imageId = fileStorageClient.uploadImageToFIleSystem(file).getBody();
        }

        Advert toSave = Advert.builder()
                .userId(userId)
                .job(job)
                .name(request.getName())
                .advertiser(request.getAdvertiser())
                .deliveryTime(request.getDeliveryTime())
                .description(request.getDescription())
                .price(request.getPrice())
                .status(AdvertStatus.OPEN)
                .imageId(imageId)
                .build();
        return saveOrUpdateAdvert(toSave);
    }

    public List<Advert> getAll() {
        var advertPoList = advertRepository.findAll();
        var jobMap = jobRepository.findAllById(
                advertPoList.stream()
                        .map(AdvertPO::getJobId).distinct().toList()
        ).stream().map(po -> modelMapper.map(po, Job.class)).collect(Collectors.toMap(Job::getId, Function.identity()));
        return advertPoList.stream().map(po -> {
            Advert advert = modelMapper.map(po, Advert.class);
            advert.setJob(jobMap.get(po.getJobId()));
            return advert;
        }).collect(Collectors.toList());
    }

    public Advert getAdvertById(String id) {
        return findAdvertById(id, true);
    }

    public List<Advert> getAdvertsByUserId(String id, Advertiser type) {
        String userId = getUserById(id).getId();
        return advertRepository.getAdvertsByUserIdAndAdvertiser(userId, type).stream()
                .map(po -> modelMapper.map(po, Advert.class))
                .collect(Collectors.toList());
    }

    public UserDto getUserById(String id) {
        return Optional.ofNullable(userServiceclient.getUserById(id).getBody())
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    @Transactional
    public Advert updateAdvertById(AdvertUpdateRequest request, MultipartFile file) {
        redisUtil.delete(getAdvertId(request.getId()));
        Advert toUpdate = findAdvertById(request.getId(), false);
        modelMapper.map(request, toUpdate);

        if (file != null) {
            String imageId = fileStorageClient.uploadImageToFIleSystem(file).getBody();
            if (imageId != null) {
                fileStorageClient.deleteImageFromFileSystem(toUpdate.getImageId());
                toUpdate.setImageId(imageId);
            }
        }

        return saveOrUpdateAdvert(toUpdate);
    }

    @Transactional
    public void deleteAdvertById(String id) {
        advertRepository.deleteById(id);
        eventPublisher.publishEvent(new ClearCacheAfterTransactionEvent(getAdvertId(id)));
    }

    public boolean authorizeCheck(String id, String principal) {
        return getUserById(getAdvertById(id).getUserId()).getUsername().equals(principal);
    }

    protected Advert findAdvertById(String id, boolean useCache) {
        if (useCache) {
            Advert advertCache = redisUtil.findObject(getAdvertId(id), Advert.class);
            if (advertCache != null) {
                return advertCache;
            }
        }

        Advert advert = advertRepository.findById(id)
                .map(po -> modelMapper.map(po, Advert.class))
                .orElseThrow(() -> new NotFoundException("Advert not found"));
        redisUtil.saveObject(getAdvertId(id), advert);
        return advert;
    }

    private String getAdvertId(String id) {
        return "advert:" + id;
    }

    private Advert saveOrUpdateAdvert(Advert advert) {
        AdvertPO savedAdvert = advertRepository.save(modelMapper.map(advert, AdvertPO.class));
//        redisUtil.saveObject(getAdvertId(savedAdvert.getId()), savedAdvert);
        eventPublisher.publishEvent(new ClearCacheAfterTransactionEvent(getAdvertId(savedAdvert.getId())));
        return modelMapper.map(savedAdvert, Advert.class);
    }
}
