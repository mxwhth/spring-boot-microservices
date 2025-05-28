package com.safalifter.jobservice.po;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity(name = "job_keys")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class JobKeyPO extends BaseEntity {
    @Column(name = "job_id")
    private String jobId;
    @Column(name = "`key`")
    private String key;
}
