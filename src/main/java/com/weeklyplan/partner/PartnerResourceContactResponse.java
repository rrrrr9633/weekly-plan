package com.weeklyplan.partner;

public record PartnerResourceContactResponse(String id, String name, String title, String phone, String wechat, String email) {
  static PartnerResourceContactResponse of(PartnerResourceContact contact) {
    return new PartnerResourceContactResponse(String.valueOf(contact.getId()), contact.getName(), contact.getTitle(), contact.getPhone(), contact.getWechat(), contact.getEmail());
  }
}
