package org.acme.common;

import javax.enterprise.context.RequestScoped;

@RequestScoped
public class CommonBean {

   public String getName() {
       return "common";
   }
}
