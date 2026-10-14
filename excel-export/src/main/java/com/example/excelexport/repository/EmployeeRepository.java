package com.example.excelexport.repository;

import com.example.excelexport.model.Employee;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Repository
public class EmployeeRepository {

    private final JdbcTemplate jdbcTemplate;

    public EmployeeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Counts matching records efficiently
     */
    public long countEmployees(String department, Long maxRows) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM employees WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (department != null && !department.isBlank()) {
            sql.append("AND department = ? ");
            params.add(department);
        }

        Long totalInDb = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        if (totalInDb == null) {
            totalInDb = 0L;
        }

        if (maxRows != null && maxRows > 0) {
            return Math.min(totalInDb, maxRows);
        }
        return totalInDb;
    }

    /**
     * Cursor-based streaming: processes rows one by one through a consumer
     * without loading all into memory.
     */
    public void streamEmployees(String department, Long maxRows, int fetchSize, Consumer<Employee> rowConsumer) {
        jdbcTemplate.setFetchSize(fetchSize);

        StringBuilder sql = new StringBuilder(
                "SELECT id, first_name, last_name, email, department, salary, join_date FROM employees WHERE 1=1 "
        );
        List<Object> params = new ArrayList<>();

        if (department != null && !department.isBlank()) {
            sql.append("AND department = ? ");
            params.add(department);
        }

        sql.append("ORDER BY id ");

        if (maxRows != null && maxRows > 0) {
            sql.append("LIMIT ? ");
            params.add(maxRows);
        }

        jdbcTemplate.query(sql.toString(), (RowCallbackHandler) rs -> {
            Employee emp = new Employee();
            emp.setId(rs.getLong("id"));
            emp.setFirstName(rs.getString("first_name"));
            emp.setLastName(rs.getString("last_name"));
            emp.setEmail(rs.getString("email"));
            emp.setDepartment(rs.getString("department"));
            emp.setSalary(rs.getBigDecimal("salary"));
            Date sqlDate = rs.getDate("join_date");
            if (sqlDate != null) {
                emp.setJoinDate(sqlDate.toLocalDate());
            }
            rowConsumer.accept(emp);
        }, params.toArray());
    }

    /**
     * Fast batch insert for bulk seeding
     */
    public void batchInsert(List<Employee> employees) {
        String sql = "INSERT INTO employees (first_name, last_name, email, department, salary, join_date) VALUES (?, ?, ?, ?, ?, ?)";

        jdbcTemplate.batchUpdate(sql, employees, 2000, (PreparedStatement ps, Employee emp) -> {
            ps.setString(1, emp.getFirstName());
            ps.setString(2, emp.getLastName());
            ps.setString(3, emp.getEmail());
            ps.setString(4, emp.getDepartment());
            ps.setBigDecimal(5, emp.getSalary());
            ps.setDate(6, Date.valueOf(emp.getJoinDate()));
        });
    }
}
