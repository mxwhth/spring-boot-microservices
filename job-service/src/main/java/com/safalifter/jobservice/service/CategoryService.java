package com.safalifter.jobservice.service;

import com.safalifter.jobservice.client.FileStorageClient;
import com.safalifter.jobservice.exc.NotFoundException;
import com.safalifter.jobservice.model.Category;
import com.safalifter.jobservice.po.CategoryPO;
import com.safalifter.jobservice.repository.CategoryRepository;
import com.safalifter.jobservice.request.category.CategoryCreateRequest;
import com.safalifter.jobservice.request.category.CategoryUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final FileStorageClient fileStorageClient;
    private final ModelMapper modelMapper;

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
        Category toUpdate = findCategoryById(request.getId());
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
    }

    protected Category findCategoryById(String id) {
        return categoryRepository.findById(id)
                .map(po -> modelMapper.map(po, Category.class))
                .orElseThrow(() -> new NotFoundException("Category not found"));
    }

    private String getCategoryCacheId(String id) {
        return "category:" + id;
    }

    private Category saveOrUpdateCategory(Category category) {
        CategoryPO savedCategoryPO =  categoryRepository.save(modelMapper.map(category, CategoryPO.class));
        return modelMapper.map(savedCategoryPO, Category.class);
    }
}
