package com.snehashis.docmind.dto;

public record LoginResponse(
        String accessToken,
        UserDto user
) {
}
