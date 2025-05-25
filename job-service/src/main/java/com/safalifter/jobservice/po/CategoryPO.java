package com.safalifter.jobservice.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import java.util.List;

@Entity(name = "categories")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CategoryPO extends BaseEntity {
    private String name;
    private String description;
    private String imageId;
}