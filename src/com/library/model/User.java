package com.library.model;

public record User(int id, String username, String fullName, String email, String passwordHash, Role role) {
}
