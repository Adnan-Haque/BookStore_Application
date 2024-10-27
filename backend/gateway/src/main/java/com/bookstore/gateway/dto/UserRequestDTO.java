package com.bookstore.gateway.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserRequestDTO {
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private List<CredentialDTO> credentials;
    private String enabled;

}
