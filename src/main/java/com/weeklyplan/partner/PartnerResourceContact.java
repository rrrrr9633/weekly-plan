package com.weeklyplan.partner;

import jakarta.persistence.*;

@Entity @Table(name = "partner_resource_contacts")
public class PartnerResourceContact {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "resource_id", nullable = false) private PartnerResource resource;
  @Column(nullable = false) private String name; private String title; private String phone; private String wechat; private String email;
  public Long getId(){return id;} public PartnerResource getResource(){return resource;} public String getName(){return name;} public String getTitle(){return title;} public String getPhone(){return phone;} public String getWechat(){return wechat;} public String getEmail(){return email;}
  public static PartnerResourceContact create(PartnerResource resource, SavePartnerContactRequest request){ PartnerResourceContact c=new PartnerResourceContact();c.resource=resource;c.name=request.name().trim();c.title=trim(request.title());c.phone=trim(request.phone());c.wechat=trim(request.wechat());c.email=trim(request.email());return c; }
  private static String trim(String value){return value==null?null:value.trim();}
}
