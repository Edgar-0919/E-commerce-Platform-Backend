package com.ecommerce.product.model.vo;

import lombok.Data;
import java.util.List;

@Data
public class SpecParamVO {

    private Long id;
    private Long groupId;
    private String name;
    private String values;
    private Integer sort;
    private List<String> parsedValues;
}