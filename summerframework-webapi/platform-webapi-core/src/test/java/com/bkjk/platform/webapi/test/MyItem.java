package com.bkjk.platform.webapi.test;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class MyItem {
    @NotBlank
    @Size(min = 2, max = 30)
    private String name;
    @NotNull
    @Min(10)
    @Max(10000)
    private Integer price;
}
