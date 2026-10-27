package com.woori.wonnie.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "customer")
public class CustomerAccount {
    @Id
    private String id;

    @Column(nullable = false, length = 50)
    private String name;

    protected CustomerAccount() {
    }

    private CustomerAccount(String id, String name) {
        this.id = id;
        this.name = name;
    }

    static CustomerAccount fromGoogle(String id, String name) {
        return new CustomerAccount(id, name);
    }

    void rename(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
