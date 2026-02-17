package com.invoiceai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class InvoiceaiApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(InvoiceaiApiApplication.class, args);
	}

}
