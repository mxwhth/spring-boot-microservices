package com.safalifter.jobservice.repository;

import com.safalifter.jobservice.model.Job;
import com.safalifter.jobservice.po.JobPO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobRepository extends JpaRepository<JobPO, String> {
    List<JobPO> getJobsByCategoryId(String id);

    List<JobPO> getJobsByKeysContainsIgnoreCase(String key);

    List<JobPO> getJobsByCategoryIdIn(List<String> ids);
}
