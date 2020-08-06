package io.quarkus.qrs.test.resource.basic.resource;

import java.util.List;

import org.junit.Assert;

public class ResourceLocatorSubresource3 implements ResourceLocatorSubresource3Interface {

   @SuppressWarnings("unused")
   @Override
   public String get(List<Double> params)
   {
      Assert.assertNotNull(params);
      Assert.assertEquals(2, params.size());
      double p1 = params.get(0);
      double p2 = params.get(1);
      return "Subresource3";
   }
}
