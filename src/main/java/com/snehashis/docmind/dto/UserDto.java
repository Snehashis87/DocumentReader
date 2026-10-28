package com.snehashis.docmind.dto;

import com.snehashis.docmind.entity.Role;

public record UserDto(
        Long id,
        String username,
        String email,
        Role role
) {
}
