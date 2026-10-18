package org.acme.common;

import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class CommonBean {

   public String getName() {
       return "common";
   }
}
