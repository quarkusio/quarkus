package io.vertx.codegen.testmodel;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.Set;

@VertxGen()
public interface AnyJavaTypeTCK {

  @GenIgnore(GenIgnore.PERMITTED_TYPE) void methodWithJavaTypeParam(Socket socket);
  @GenIgnore(GenIgnore.PERMITTED_TYPE) void methodWithListOfJavaTypeParam(List<Socket> socketList);
  @GenIgnore(GenIgnore.PERMITTED_TYPE) void methodWithSetOfJavaTypeParam(Set<Socket> socketSet);
  @GenIgnore(GenIgnore.PERMITTED_TYPE) void methodWithMapOfJavaTypeParam(Map<String, Socket> socketMap);

  @GenIgnore(GenIgnore.PERMITTED_TYPE) Socket methodWithJavaTypeReturn();
  @GenIgnore(GenIgnore.PERMITTED_TYPE) List<Socket> methodWithListOfJavaTypeReturn();
  @GenIgnore(GenIgnore.PERMITTED_TYPE) Set<Socket> methodWithSetOfJavaTypeReturn();
  @GenIgnore(GenIgnore.PERMITTED_TYPE) Map<String, Socket> methodWithMapOfJavaTypeReturn();

  @GenIgnore(GenIgnore.PERMITTED_TYPE) void methodWithHandlerJavaTypeParam(Handler<Socket> socketHandler);
  @GenIgnore(GenIgnore.PERMITTED_TYPE) void methodWithHandlerListOfJavaTypeParam(Handler<List<Socket>> socketListHandler);
  @GenIgnore(GenIgnore.PERMITTED_TYPE) void methodWithHandlerSetOfJavaTypeParam(Handler<Set<Socket>> socketSetHandler);
  @GenIgnore(GenIgnore.PERMITTED_TYPE) void methodWithHandlerMapOfJavaTypeParam(Handler<Map<String, Socket>> socketMapHandler);

  @GenIgnore(GenIgnore.PERMITTED_TYPE) void methodWithHandlerAsyncResultJavaTypeParam(Handler<AsyncResult<Socket>> socketHandler);
  @GenIgnore(GenIgnore.PERMITTED_TYPE) void methodWithHandlerAsyncResultListOfJavaTypeParam(Handler<AsyncResult<List<Socket>>> socketListHandler);
  @GenIgnore(GenIgnore.PERMITTED_TYPE) void methodWithHandlerAsyncResultSetOfJavaTypeParam(Handler<AsyncResult<Set<Socket>>> socketSetHandler);
  @GenIgnore(GenIgnore.PERMITTED_TYPE) void methodWithHandlerAsyncResultMapOfJavaTypeParam(Handler<AsyncResult<Map<String, Socket>>> socketMapHandler);
}
