package io.vertx.lang.axle.it;

import java.util.Objects;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class WineAndCheese {

  private String wine;
  private String cheese;

  public String getWine() {
    return wine;
  }

  public WineAndCheese setWine(String wine) {
    this.wine = wine;
    return this;
  }

  public String getCheese() {
    return cheese;
  }

  public WineAndCheese setCheese(String cheese) {
    this.cheese = cheese;
    return this;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof WineAndCheese) {
      WineAndCheese that = (WineAndCheese) obj;
      return Objects.equals(wine, that.wine) && Objects.equals(cheese, that.cheese);
    }
    return false;
  }
}
