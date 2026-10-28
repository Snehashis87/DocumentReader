package com.snehashis.docmind.dto;

// What is record in Java?
// -> In Java, a `record` is a special kind of class that is used to model immutable data. It was introduced in Java 14 as a preview feature and became a standard feature in Java 16. Records provide a concise way to define classes that primarily hold data, automatically generating boilerplate code such as constructors, `equals()`, `hashCode()`, and `toString()` methods. A record is defined using the `record` keyword, and its fields are declared in the record header. Once created, the fields of a record cannot be modified, making it a good choice for representing simple data carriers or value objects.
public record LoginRequest(
        String username,
        String password
) {
}
