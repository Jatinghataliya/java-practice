package com.example.excelexport.service;

import com.example.excelexport.model.Employee;
import com.example.excelexport.repository.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class EmployeeDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(EmployeeDataSeeder.class);

    private final EmployeeRepository employeeRepository;

    @Value("${app.export.seed-sample-records:50000}")
    private int defaultSeedCount;

    private final String[] FIRST_NAMES = {"James", "Mary", "Robert", "Patricia", "John", "Jennifer", "Michael", "Linda", "David", "Elizabeth", "William", "Barbara", "Richard", "Susan", "Joseph", "Jessica", "Thomas", "Sarah", "Charles", "Karen"};
    private final String[] LAST_NAMES = {"Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis", "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson", "Thomas", "Taylor", "Moore", "Jackson", "Martin"};
    private final String[] DEPARTMENTS = {"Engineering", "Product", "Human Resources", "Finance", "Marketing", "Sales", "Legal", "Operations", "Customer Success", "Security"};

    public EmployeeDataSeeder(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    public void run(String... args) {
        if (defaultSeedCount > 0) {
            log.info("Auto-seeding {} sample employee records for quick testing...", defaultSeedCount);
            seedEmployees(defaultSeedCount);
        }
    }

    public long seedEmployees(int count) {
        long startTime = System.currentTimeMillis();
        int batchSize = 5000;
        List<Employee> batch = new ArrayList<>(batchSize);
        Random random = new Random();
        LocalDate baseDate = LocalDate.of(2020, 1, 1);

        for (int i = 1; i <= count; i++) {
            String firstName = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
            String lastName = LAST_NAMES[random.nextInt(LAST_NAMES.length)];
            String department = DEPARTMENTS[random.nextInt(DEPARTMENTS.length)];
            String email = firstName.toLowerCase() + "." + lastName.toLowerCase() + i + "@company.com";
            BigDecimal salary = BigDecimal.valueOf(50000 + random.nextInt(120000)).setScale(2, RoundingMode.HALF_UP);
            LocalDate joinDate = baseDate.plusDays(random.nextInt(1500));

            Employee emp = new Employee(null, firstName, lastName, email, department, salary, joinDate);
            batch.add(emp);

            if (batch.size() >= batchSize || i == count) {
                employeeRepository.batchInsert(batch);
                batch.clear();
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("Successfully seeded {} employee records in {} ms.", count, duration);
        return duration;
    }
}
