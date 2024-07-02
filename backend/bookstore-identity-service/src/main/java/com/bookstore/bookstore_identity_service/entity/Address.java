package com.bookstore.bookstore_identity_service.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Address {
    private String roomNo;
    private String house;
    private String street;
    private String locality;
    private Integer pincode;
    private String state;
    private String country;
}
