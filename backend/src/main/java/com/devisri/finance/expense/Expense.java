package com.devisri.finance.expense;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.fasterxml.jackson.annotation.JsonIgnore;

/** Same "expenses" collection the old Node.js server used. */
@Document("expenses")
public class Expense {

    @Id
    private String id;
    /** Owner of the expense. Stored as an ObjectId, exactly like the old data. */
    @JsonIgnore
    @Field(targetType = FieldType.OBJECT_ID)
    private String user;
    private String title;
    private double amount;
    private String category;
    private Instant date;
    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;

    public Expense() {
    }

    public Expense(String user, String title, double amount, String category, Instant date) {
        this.user = user;
        this.title = title;
        this.amount = amount;
        this.category = category;
        this.date = date;
    }

    public String getId() { return id; }
    public String getUser() { return user; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Instant getDate() { return date; }
    public void setDate(Instant date) { this.date = date; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
