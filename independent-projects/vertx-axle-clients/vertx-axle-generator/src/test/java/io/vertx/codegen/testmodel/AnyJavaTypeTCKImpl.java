package io.vertx.codegen.testmodel;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;

import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

public class AnyJavaTypeTCKImpl implements AnyJavaTypeTCK {

  @Override
  public void methodWithJavaTypeParam(Socket socket) {
    assertNotNull(socket);
  }

  @Override
  public void methodWithListOfJavaTypeParam(List<Socket> socketList) {
    for (Socket socket : socketList) {
      assertNotNull(socket);
    }
  }

  @Override
  public void methodWithSetOfJavaTypeParam(Set<Socket> socketSet) {
    for (Socket socket : socketSet) {
      assertNotNull(socket);
    }
  }

  @Override
  public void methodWithMapOfJavaTypeParam(Map<String, Socket> socketMap) {
    for (Map.Entry<String, Socket> stringSocketEntry : socketMap.entrySet()) {
      assertNotNull(stringSocketEntry.getValue());
    }
  }

  @Override
  public Socket methodWithJavaTypeReturn() {
    return new Socket();
  }

  @Override
  public List<Socket> methodWithListOfJavaTypeReturn() {
    Socket socket = new Socket();
    ArrayList<Socket> sockets = new ArrayList<>();
    sockets.add(socket);
    return sockets;
  }

  @Override
  public Set<Socket> methodWithSetOfJavaTypeReturn() {
    Socket socket = new Socket();
    Set<Socket> sockets = new HashSet<>();
    sockets.add(socket);
    return sockets;
  }

  @Override
  public Map<String, Socket> methodWithMapOfJavaTypeReturn() {
    Socket socket = new Socket();
    Map<String, Socket> sockets = new HashMap<>();
    sockets.put("1", socket);
    return sockets;
  }

  @Override
  public void methodWithHandlerJavaTypeParam(Handler<Socket> socketHandler) {
    assertNotNull(socketHandler);
    socketHandler.handle(new Socket());
  }

  @Override
  public void methodWithHandlerListOfJavaTypeParam(Handler<List<Socket>> socketListHandler) {
    assertNotNull(socketListHandler);
    Socket socket = new Socket();
    ArrayList<Socket> sockets = new ArrayList<>();
    sockets.add(socket);
    socketListHandler.handle(sockets);
  }

  @Override
  public void methodWithHandlerSetOfJavaTypeParam(Handler<Set<Socket>> socketSetHandler) {
    assertNotNull(socketSetHandler);
    Socket socket = new Socket();
    Set<Socket> sockets = new HashSet<>();
    sockets.add(socket);
    socketSetHandler.handle(sockets);
  }

  @Override
  public void methodWithHandlerMapOfJavaTypeParam(Handler<Map<String, Socket>> socketMapHandler) {
    assertNotNull(socketMapHandler);
    Socket socket = new Socket();
    Map<String, Socket> sockets = new HashMap<>();
    sockets.put("1", socket);
    socketMapHandler.handle(sockets);
  }

  @Override
  public void methodWithHandlerAsyncResultJavaTypeParam(Handler<AsyncResult<Socket>> socketHandler) {
    assertNotNull(socketHandler);
    Socket socket = new Socket();
    socketHandler.handle(Future.succeededFuture(socket));
  }

  @Override
  public void methodWithHandlerAsyncResultListOfJavaTypeParam(Handler<AsyncResult<List<Socket>>> socketListHandler) {
    assertNotNull(socketListHandler);
    Socket socket = new Socket();
    ArrayList<Socket> sockets = new ArrayList<>();
    sockets.add(socket);
    socketListHandler.handle(Future.succeededFuture(sockets));
  }

  @Override
  public void methodWithHandlerAsyncResultSetOfJavaTypeParam(Handler<AsyncResult<Set<Socket>>> socketSetHandler) {
    assertNotNull(socketSetHandler);
    Socket socket = new Socket();
    Set<Socket> sockets = new HashSet<>();
    sockets.add(socket);
    socketSetHandler.handle(Future.succeededFuture(sockets));
  }

  @Override
  public void methodWithHandlerAsyncResultMapOfJavaTypeParam(Handler<AsyncResult<Map<String, Socket>>> socketMapHandler) {
    assertNotNull(socketMapHandler);
    Socket socket = new Socket();
    Map<String, Socket> sockets = new HashMap<>();
    sockets.put("1", socket);
    socketMapHandler.handle(Future.succeededFuture(sockets));
  }
}
