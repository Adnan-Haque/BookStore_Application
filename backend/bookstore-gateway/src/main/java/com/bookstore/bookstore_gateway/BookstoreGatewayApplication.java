package com.bookstore.bookstore_gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class BookstoreGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(BookstoreGatewayApplication.class, args);
	}

}