package com.wesign.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDataDto {

    private String firstname;
    private String lastName;
    private String series;
    private String number;
    private String cnp;
}
