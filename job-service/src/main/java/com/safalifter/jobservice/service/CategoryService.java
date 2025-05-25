package com.safalifter.jobservice.service;

import com.safalifter.jobservice.client.FileStorageClient;
import com.safalifter.jobservice.exc.NotFoundException;
import com.safalifter.jobservice.model.Category;
import com.safalifter.jobservice.model.Job;
import com.safalifter.jobservice.po.CategoryPO;
import com.safalifter.jobservice.po.JobPO;
import com.safalifter.jobservice.repository.CategoryRepository;
import com.safalifter.jobservice.repository.JobRepository;
import com.safalifter.jobservice.request.category.CategoryCreateRequest;
import com.safalifter.jobservice.request.category.CategoryUpdateRequest;
import com.safalifter.jobservice.transaction.ClearCacheAfterTransactionEvent;
import com.safalifter.jobservice.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final FileStorageClient fileStorageClient;
    private final ModelMapper modelMapper;
    private final RedisUtil redisUtil;
    private final ApplicationEventPublisher eventPublisher;
    private final JobRepository jobRepository;

    @Transactional
    public Category createCategory(CategoryCreateRequest request, MultipartFile file) {
        String imageId = null;

        if (file != null)
            imageId = fileStorageClient.uploadImageToFIleSystem(file).getBody();

        return saveOrUpdateCategory(Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .imageId(imageId)
                .build());
    }

    public List<Category> getAll() {
        var categories = categoryRepository.findAll();
        if (categories.isEmpty()) {
            return List.of();
        }

        return categories.stream().map(po -> modelMapper.map(po, Category.class)).collect(Collectors.toList());
    }

    public Category getCategoryById(String id) {
        return findCategoryById(id);
    }

    @Transactional
    public Category updateCategoryById(CategoryUpdateRequest request, MultipartFile file) {
        redisUtil.delete(getCategoryCacheId(request.getId()));

        Category toUpdate = findCategoryById(request.getId(), false);
        modelMapper.map(request, toUpdate);

        if (file != null) {
            String imageId = fileStorageClient.uploadImageToFIleSystem(file).getBody();
            if (imageId != null) {
                fileStorageClient.deleteImageFromFileSystem(toUpdate.getImageId());
                toUpdate.setImageId(imageId);
            }
        }

        return saveOrUpdateCategory(toUpdate);
    }

    @Transactional
    public void deleteCategoryById(String id) {
        categoryRepository.deleteById(id);
        eventPublisher.publishEvent(new ClearCacheAfterTransactionEvent(getCategoryCacheId(id)));
    }

    protected Category findCategoryById(String id) {
        return findCategoryById(id, true);
    }

    protected Category findCategoryById(String id, boolean useCache) {
        if (useCache) {
            Category categoryCache = redisUtil.findObject(getCategoryCacheId(id), Category.class);
            if (categoryCache != null) {
                return categoryCache;
            }
        }
        Category category = categoryRepository.findById(id)
                .map(po -> modelMapper.map(po, Category.class))
                .orElseThrow(() -> new NotFoundException("Category not found"));
        redisUtil.saveObject(getCategoryCacheId(id), category);
        return category;
    }

    private String getCategoryCacheId(String id) {
        return "category:" + id;
    }

    private Category saveOrUpdateCategory(Category category) {
        CategoryPO savedCategoryPO =  categoryRepository.save(modelMapper.map(category, CategoryPO.class));
//        redisUtil.saveObject(getCategoryCacheId(savedCategory.getId()), savedCategory);
        eventPublisher.publishEvent(new ClearCacheAfterTransactionEvent(getCategoryCacheId(savedCategoryPO.getId())));
        return modelMapper.map(savedCategoryPO, Category.class);
    }
}
