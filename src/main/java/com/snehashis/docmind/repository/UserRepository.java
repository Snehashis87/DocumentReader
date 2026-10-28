package com.snehashis.docmind.repository;

import com.snehashis.docmind.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // What is Optional in Java?
    // -> In Java, `Optional` is a container object which may or may not contain a non-null value. It is used to represent the presence or absence of a value and helps to avoid `NullPointerException`. Instead of returning `null`, methods can return an `Optional` object, which can either be empty (indicating no value) or contain a value. This encourages developers to explicitly handle the case where a value might be absent, leading to more robust and readable code.
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);

    Boolean existsByUsername(String username);
}
