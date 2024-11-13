package com.bookstore.gateway.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CredentialsDTO {

    @NotNull(message = "Username cannot be null")
    @NotEmpty(message = "Username cannot be empty")
    private String username;
    private String firstName;
    private String lastName;
    private Boolean enabled;

    @NotNull(message = "At least 1 Credential should be there")
    @NotEmpty(message = "At least 1 Credential should be there")
    private List<Credentials> credentials;
//    private Map<String, String> attributes;
//    private List<String> groups;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Roles roles;
//    private List<String> requiredActions;
}
