package com.safalifter.jobservice.service;

import com.safalifter.jobservice.client.FileStorageClient;
import com.safalifter.jobservice.exc.NotFoundException;
import com.safalifter.jobservice.model.Category;
import com.safalifter.jobservice.model.Job;
import com.safalifter.jobservice.po.JobPO;
import com.safalifter.jobservice.repository.CategoryRepository;
import com.safalifter.jobservice.repository.JobRepository;
import com.safalifter.jobservice.request.job.JobCreateRequest;
import com.safalifter.jobservice.request.job.JobUpdateRequest;
import com.safalifter.jobservice.transaction.ClearCacheAfterTransactionEvent;
import com.safalifter.jobservice.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobService {
    private final JobRepository jobRepository;
    private final FileStorageClient fileStorageClient;
    private final ModelMapper modelMapper;
    private final RedisUtil redisUtil;
    private final RedissonClient redissonClient;
    private final ApplicationEventPublisher eventPublisher;
    private final CategoryRepository categoryRepository;

    @Transactional
    public Job createJob(JobCreateRequest request, MultipartFile file) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .map(po -> modelMapper.map(po, Category.class))
                .orElseThrow(() -> new NotFoundException("Category not found"));

        String imageId = null;

        if (file != null) {
            imageId = fileStorageClient.uploadImageToFIleSystem(file).getBody();
        }

        return saveOrUpdateCategory(Job.builder()
                .name(request.getName())
                .description(request.getDescription())
                .category(category)
                .keys(Optional.of(List.of(request.getKeys()))
                        .orElse(new ArrayList<>()))
                .imageId(imageId)
                .build());
    }

    public List<Job> getAll() {
        var jobPoList = jobRepository.findAll();
        var categoryMap =
                categoryRepository.findAllById(jobPoList.stream().map(JobPO::getCategoryId).distinct().toList())
                        .stream()
                        .map(categoryPO -> modelMapper.map(categoryPO, Category.class))
                        .collect(Collectors.toMap(Category::getId, Function.identity()));

        return jobPoList.stream()
                .map(jobPo -> {
                    Job job = modelMapper.map(jobPo, Job.class);
                    job.setCategory(categoryMap.get(jobPo.getCategoryId()));
                    return job;
                })
                .collect(Collectors.toList());
    }

    public Job getJobById(String id) {
        return findJobById(id);
    }

    @Transactional
    public Job updateJob(JobUpdateRequest request, MultipartFile file) {
        RLock lock = redissonClient.getLock("update:" + getJobCacheId(request.getId()));
        if (!lock.tryLock()) {
            try {
                redisUtil.delete(getJobCacheId(request.getId()));

                Job toUpdate = findJobById(request.getCategoryId(), false);
                modelMapper.map(request, toUpdate);

                if (file != null) {
                    String imageId = fileStorageClient.uploadImageToFIleSystem(file).getBody();
                    if (imageId != null) {
                        fileStorageClient.deleteImageFromFileSystem(toUpdate.getImageId());
                        toUpdate.setImageId(imageId);
                    }
                }

                return saveOrUpdateCategory(toUpdate);
            } finally {
                lock.unlock();
            }
        } else {
            throw new RuntimeException("Concurrent Job Update!!! job-id: %s".formatted(request.getId()));
        }
    }

    @Transactional
    public void deleteJobById(String id) {
        jobRepository.deleteById(id);
        eventPublisher.publishEvent(new ClearCacheAfterTransactionEvent(getJobCacheId(id)));
    }

    public List<Job> getJobsByCategoryId(String categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .map(po -> modelMapper.map(po, Category.class))
                .orElseThrow(() -> new NotFoundException("Category not found"));
        return jobRepository.getJobsByCategoryId(categoryId).stream()
                .map(po -> modelMapper.map(po, Job.class))
                .peek(job -> job.setCategory(category))
                .collect(Collectors.toList());
    }

    public List<Job> getJobsThatFitYourNeeds(String needs) {
        String[] keys = needs.replaceAll("\"", "").split(" ");
        HashMap<String, Integer> map = new HashMap<>();
        Arrays.stream(keys).forEach(key -> jobRepository.getJobsByKeysContainsIgnoreCase(key)
                .forEach(job -> {
                    if (map.containsKey(job.getId())) {
                        int count = map.get(job.getId());
                        map.put(job.getId(), count + 1);
                    } else {
                        map.put(job.getId(), 1);
                    }
                }));
        return map.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .map(entry -> findJobById(entry.getKey()))
                .collect(Collectors.toList());
    }

    protected Job findJobById(String id) {
        return findJobById(id, true);
    }

    protected Job findJobById(String id, boolean useCache) {
        if (useCache) {
            Job jobCache = redisUtil.findObject(getJobCacheId(id), Job.class);
            if (jobCache != null) {
                return jobCache;
            }
        }
        Job job = jobRepository.findById(id)
                .map(po -> modelMapper.map(po, Job.class))
                .orElseThrow(() -> new NotFoundException("Job not found"));
        redisUtil.saveObject(getJobCacheId(id), job);
        return job;
    }

    private Job saveOrUpdateCategory(Job job) {
        JobPO savedJob = jobRepository.save(modelMapper.map(job, JobPO.class));
//        redisUtil.saveObject(getJobCacheId(savedJob.getId()), savedJob);
        eventPublisher.publishEvent(new ClearCacheAfterTransactionEvent(getJobCacheId(savedJob.getId())));
        return modelMapper.map(savedJob, Job.class);
    }

    private String getJobCacheId(String id) {
        return "job:" + id;
    }
}
