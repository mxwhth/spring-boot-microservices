package com.safalifter.jobservice.repository;

import com.safalifter.jobservice.po.JobKeyPO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobKeyRepository extends JpaRepository<JobKeyPO, String> {
    List<JobKeyPO> getJobKeysByKeyContainsIgnoreCase(String key);

    List<JobKeyPO> getAllByKeyIn(List<String> keyList);

    void deleteAllByJobId(String jobId);

    List<JobKeyPO> getAllByJobId(String jobId);
}
