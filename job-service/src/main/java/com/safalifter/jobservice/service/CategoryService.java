package com.safalifter.jobservice.service;

import com.safalifter.jobservice.client.FileStorageClient;
import com.safalifter.jobservice.exc.NotFoundException;
import com.safalifter.jobservice.model.Category;
import com.safalifter.jobservice.repository.CategoryRepository;
import com.safalifter.jobservice.request.category.CategoryCreateRequest;
import com.safalifter.jobservice.request.category.CategoryUpdateRequest;
import com.safalifter.jobservice.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final FileStorageClient fileStorageClient;
    private final ModelMapper modelMapper;
    private final RedisUtil redisUtil;

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
        return categoryRepository.findAll();
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

    public void deleteCategoryById(String id) {
        redisUtil.delete(getCategoryCacheId(id));
        categoryRepository.deleteById(id);
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
                .orElseThrow(() -> new NotFoundException("Category not found"));
        redisUtil.saveObject(getCategoryCacheId(id), category);
        return category;
    }

    private String getCategoryCacheId(String id) {
        return "category:" + id;
    }

    private Category saveOrUpdateCategory(Category category) {
        Category savedCategory =  categoryRepository.save(category);
        redisUtil.saveObject(getCategoryCacheId(savedCategory.getId()), savedCategory);
        return savedCategory;
    }
}
