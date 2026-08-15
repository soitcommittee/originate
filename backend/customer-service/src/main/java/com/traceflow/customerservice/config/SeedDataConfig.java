package com.traceflow.customerservice.config;

import com.traceflow.customerservice.domain.Customer;
import com.traceflow.customerservice.repositories.CustomerRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

@Configuration
public class SeedDataConfig {
    @Bean
    CommandLineRunner seedCustomers(CustomerRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                repository.saveAll(List.of(
                        customer("Aisha Rahman", "aisha.rahman@originate.demo", "+60 12-345 6789", "8500.00"),
                        customer("Daniel Lim", "daniel.lim@originate.demo", "+60 16-222 4311", "6200.00"),
                        customer("Mei Ling Tan", "mei.tan@originate.demo", "+60 17-880 2140", "11200.00"),
                        customer("Arjun Nair", "arjun.nair@originate.demo", "+60 12-707 1844", "9800.00"),
                        customer("Nur Izzati Hassan", "izzati.hassan@originate.demo", "+60 13-461 9205", "5400.00"),
                        customer("Jason Wong", "jason.wong@originate.demo", "+60 19-338 7120", "15800.00"),
                        customer("Siti Hajar Ahmad", "siti.hajar@originate.demo", "+60 11-286 9304", "7300.00"),
                        customer("Kelvin Lee", "kelvin.lee@originate.demo", "+60 14-552 6081", "4600.00"),
                        customer("Priya Menon", "priya.menon@originate.demo", "+60 12-934 1028", "12800.00"),
                        customer("Farid Iskandar", "farid.iskandar@originate.demo", "+60 18-623 4450", "6700.00"),
                        customer("Amanda Goh", "amanda.goh@originate.demo", "+60 16-784 2193", "9100.00"),
                        customer("Hafiz Zulkifli", "hafiz.zulkifli@originate.demo", "+60 11-392 7816", "3900.00")
                ));
            }
        };
    }

    private Customer customer(String name, String email, String phone, String monthlyIncome) {
        return new Customer(name, email, phone, new BigDecimal(monthlyIncome));
    }
}
