package com.safalifter.jobservice.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import java.util.Collections;
import java.util.List;

@Entity(name = "jobs")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class JobPO extends BaseEntity {
    private String name;
    private String description;
    private String imageId;

    private String categoryId;

    @ElementCollection
    @CollectionTable(name = "job_keys", joinColumns = @JoinColumn(name = "job_id"))
    @Column(name = "key")
    private List<String> keys = Collections.emptyList();
}
