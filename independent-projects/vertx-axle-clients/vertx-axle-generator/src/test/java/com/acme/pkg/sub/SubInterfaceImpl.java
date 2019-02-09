package com.acme.pkg.sub;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class SubInterfaceImpl implements SubInterface {

  @Override
  public String reverse(String s) {
    return new StringBuilder(s).reverse().toString();
  }
}
