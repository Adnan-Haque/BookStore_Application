package com.bookstore.books_service.controller;

import com.bookstore.books_service.entity.Book;
import com.bookstore.books_service.service.BookService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }


    @GetMapping("/get-book/{id}")
    public Mono<Book> getBook(@RequestParam String id){
        return bookService.getBook(id);
    }

    @GetMapping("/get-all-books")
    public Flux<Book> getAllBooks(){
        return bookService.getAllBooks();
    }

    @PostMapping("/add-book")
    public Mono<String> addBook(@RequestBody Book book){
        return bookService.addBook(book);
    }

    @DeleteMapping("/delete-book/{id}")
    public Mono<String> deleteBook(@RequestParam String id){
        return bookService.deleteBook(id);
    }
}
