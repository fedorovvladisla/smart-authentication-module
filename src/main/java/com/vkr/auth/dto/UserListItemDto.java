package com.vkr.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserListItemDto {
    private String id;
    private String username;
    private String role;
    private boolean blocked;
}