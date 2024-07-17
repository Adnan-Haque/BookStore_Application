package com.bookstore.books_service.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("Books")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Book {
    @Id
    String id;
    String name;
    Integer quantity;
    Long sold;
    String description;
    boolean renting;
    boolean buying;
    boolean membersOnly;
    private boolean adult;
}
