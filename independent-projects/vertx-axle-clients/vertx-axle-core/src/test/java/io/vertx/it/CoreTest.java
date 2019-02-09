package io.vertx.it;

import io.reactivex.Flowable;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.impl.Utils;
import io.vertx.axle.core.Vertx;
import io.vertx.axle.core.file.AsyncFile;
import io.vertx.test.core.TestUtils;
import io.vertx.test.core.VertxTestBase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;

public class CoreTest extends VertxTestBase {

  private static final String DEFAULT_FILE_PERMS = "rw-r--r--";

  private Vertx vertx;
  private String pathSep;
  private String testDir;

  @Rule
  public TemporaryFolder testFolder = new TemporaryFolder();

  @Override
  public void setUp() throws Exception {
    super.setUp();
    vertx = new Vertx(super.vertx);
    java.nio.file.FileSystem fs = FileSystems.getDefault();
    pathSep = fs.getSeparator();
    File ftestDir = testFolder.newFolder();
    testDir = ftestDir.toString();
  }

  @Test
  public void testAsyncFile() throws Exception {
    String fileName = "some-file.dat";
    int chunkSize = 1000;
    int chunks = 10;
    byte[] expected = TestUtils.randomAlphaString(chunkSize * chunks).getBytes();
    createFile(fileName, expected);
    vertx.fileSystem().open(testDir + pathSep + fileName, new OpenOptions()).whenComplete((file, err) -> {
      assertNull(err);
      subscribe(expected, file, 3);
    });
    await();
  }

  private void subscribe(byte[] expected, AsyncFile file, int times) {
    file.setReadPos(0);
    Flowable<Buffer> flowable = Flowable.fromPublisher(file.toPublisher()).map(io.vertx.axle.core.buffer.Buffer::getDelegate);
    Buffer actual = Buffer.buffer();
    flowable.subscribe(
      actual::appendBuffer,
      this::fail,
      () -> {
        assertEquals(Buffer.buffer(expected), actual);
        if (times > 0) {
          subscribe(expected, file, times - 1);
        } else {
          testComplete();
        }
      });
  }

  private void createFile(String fileName, byte[] bytes) throws Exception {
    File file = new File(testDir, fileName);
    Path path = Paths.get(file.getCanonicalPath());
    Files.write(path, bytes);
    setDefaultPerms(path);
  }

  private void setDefaultPerms(Path path) {
    if (!Utils.isWindows()) {
      try {
        Files.setPosixFilePermissions( path, PosixFilePermissions.fromString(DEFAULT_FILE_PERMS) );
      }
      catch(IOException e) {
        throw new RuntimeException(e.getMessage());
      }
    }
  }
}
