package com.bookstore.books_service.service;

import com.bookstore.books_service.entity.Book;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface BookService {
    public Mono<Book> getBook(String id);

    public Flux<Book> getAllBooks();

    public Mono<String> addBook(Book book);

    Mono<String> deleteBook(String id);
}
