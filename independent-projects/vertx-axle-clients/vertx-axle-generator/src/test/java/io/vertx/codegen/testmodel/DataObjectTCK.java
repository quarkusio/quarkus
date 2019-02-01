package io.vertx.codegen.testmodel;

import io.vertx.codegen.annotations.VertxGen;

/**
 * todo:
 * - Buffer support
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
@VertxGen
public interface DataObjectTCK {

  DataObjectWithValues getDataObjectWithValues();

  void setDataObjectWithValues(DataObjectWithValues dataObject);

  DataObjectWithLists getDataObjectWithLists();

  void setDataObjectWithLists(DataObjectWithLists dataObject);

  DataObjectWithMaps getDataObjectWithMaps();

  void setDataObjectWithMaps(DataObjectWithMaps dataObject);

  void methodWithOnlyJsonObjectConstructorDataObject(DataObjectWithOnlyJsonObjectConstructor dataObject);

  void setDataObjectWithBuffer(DataObjectWithNestedBuffer dataObject);

  void setDataObjectWithListAdders(DataObjectWithListAdders dataObject);

  void setDataObjectWithMapAdders(DataObjectWithMapAdders dataObject);

  void setDataObjectWithRecursion(DataObjectWithRecursion dataObject);

}
