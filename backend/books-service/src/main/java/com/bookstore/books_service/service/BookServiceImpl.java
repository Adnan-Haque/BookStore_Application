package com.bookstore.books_service.service;

import com.bookstore.books_service.entity.Book;
import com.bookstore.books_service.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class BookServiceImpl implements BookService{

    private final BookRepository bookRepository;

    @Autowired
    private BookServiceImpl(BookRepository bookRepository){
        this.bookRepository = bookRepository;
    }

    @Override
    public Mono<Book> getBook(String id) {
        return bookRepository.findById(id);
    }

    @Override
    public Flux<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    @Override
    public Mono<String> addBook(Book book) {
        return bookRepository.save(book).then(Mono.just("Book Saved Successfully"));
    }

    @Override
    public Mono<String> deleteBook(String id) {
        return bookRepository.deleteById(id).then(Mono.just("Book Deleted Successfully"));
    }
}
