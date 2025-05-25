package com.safalifter.jobservice.model;

import lombok.*;

import javax.persistence.*;
import java.util.Collections;
import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Job extends BaseEntity {
    private String name;
    private String description;
    private String imageId;

    private Category category;

    private List<String> keys = Collections.emptyList();
}
