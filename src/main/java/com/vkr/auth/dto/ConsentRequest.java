package com.vkr.auth.dto;

import lombok.Data;

@Data
public class ConsentRequest {
    private boolean agreed;          // пользователь поставил галочку
    private String consentVersion;   // версия согласия (например, "1.0")
}