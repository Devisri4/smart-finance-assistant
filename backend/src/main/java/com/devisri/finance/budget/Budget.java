package com.devisri.finance.budget;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.fasterxml.jackson.annotation.JsonIgnore;

/** A monthly spending limit for one category, e.g. "Food: at most 5000 per month". */
@Document("budgets")
public class Budget {

    @Id
    private String id;
    @JsonIgnore
    @Field(targetType = FieldType.OBJECT_ID)
    private String user;
    private String category;
    private double monthlyLimit;
    @CreatedDate
    private Instant createdAt;

    public Budget() {
    }

    public Budget(String user, String category, double monthlyLimit) {
        this.user = user;
        this.category = category;
        this.monthlyLimit = monthlyLimit;
    }

    public String getId() { return id; }
    public String getUser() { return user; }
    public String getCategory() { return category; }
    public double getMonthlyLimit() { return monthlyLimit; }
    public void setMonthlyLimit(double monthlyLimit) { this.monthlyLimit = monthlyLimit; }
    public Instant getCreatedAt() { return createdAt; }
}
