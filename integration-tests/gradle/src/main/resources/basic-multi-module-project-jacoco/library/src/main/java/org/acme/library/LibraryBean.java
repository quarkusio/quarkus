package org.acme.library;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class LibraryBean {

   public String getName() {
       return "library";
   }
}
