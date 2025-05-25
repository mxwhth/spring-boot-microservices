package com.safalifter.jobservice.repository;

import com.safalifter.jobservice.model.Category;
import com.safalifter.jobservice.po.CategoryPO;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<CategoryPO, String> {
}
