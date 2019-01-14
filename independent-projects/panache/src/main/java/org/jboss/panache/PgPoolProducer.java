package org.jboss.panache;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.reactiverse.pgclient.PgPoolOptions;
import io.reactiverse.reactivex.pgclient.PgClient;
import io.reactiverse.reactivex.pgclient.PgPool;

@Singleton
public class PgPoolProducer {
	
    @Inject
    @ConfigProperty(name = "shamrock.datasource.host", defaultValue = "localhost")
    private String host;

    @Inject
    @ConfigProperty(name = "shamrock.datasource.port")
    private Integer port;

    @Inject
    @ConfigProperty(name = "shamrock.datasource.username")
    private String user;

    @Inject
    @ConfigProperty(name = "shamrock.datasource.database")
    private String database;

    @Inject
    @ConfigProperty(name = "shamrock.datasource.password")
    private String pass;

    @Inject
    @ConfigProperty(name = "shamrock.datasource.maxSize")
    private Integer maxSize;

    @Singleton
	@Produces
	public PgPool getClient() {
		PgPoolOptions options = new PgPoolOptions()
				  .setHost(host)
				  .setDatabase(database)
				  .setUser(user)
				  .setPassword(pass);
		if(maxSize != null)
		    options.setMaxSize(maxSize);
		if(port != null)
		    options.setPort(port);

		// Create the client pool
        PgPool pool = PgClient.pool(options);
		
		// FIXME: make configurable?
		InputStream sql = Thread.currentThread().getContextClassLoader().getResourceAsStream("META-INF/load.sql");
		if(sql != null) {
		    try(BufferedReader reader = new BufferedReader(new InputStreamReader(sql, "UTF-8"))){
		        String line;
		        while((line = reader.readLine()) != null){
		            line = line.trim();
		            if(line.startsWith("--") || line.isEmpty())
		                continue;
		            // FIXME: multi-line SQL?
		            pool.rxQuery(line).blockingGet();
		        }
            } catch (IOException e) {
                e.printStackTrace();
            }
		}

		return pool;
	}
}
