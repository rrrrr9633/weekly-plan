package com.weeklyplan.partner;
import jakarta.validation.constraints.NotBlank; import jakarta.validation.constraints.Size;
public record SavePartnerContactRequest(@NotBlank @Size(max=64) String name, @Size(max=64) String title, @Size(max=32) String phone, @Size(max=64) String wechat, @Size(max=128) String email) {}
