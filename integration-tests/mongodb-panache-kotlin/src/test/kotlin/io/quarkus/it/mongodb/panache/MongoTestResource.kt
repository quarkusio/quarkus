package io.quarkus.it.mongodb.panache

import de.flapdoodle.embed.mongo.Command
import de.flapdoodle.embed.mongo.MongodExecutable
import de.flapdoodle.embed.mongo.MongodStarter
import de.flapdoodle.embed.mongo.config.IMongodConfig
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder
import de.flapdoodle.embed.mongo.config.Net
import de.flapdoodle.embed.mongo.config.RuntimeConfigBuilder
import de.flapdoodle.embed.mongo.distribution.Version
import de.flapdoodle.embed.process.config.io.ProcessOutput
import de.flapdoodle.embed.process.io.Processors
import de.flapdoodle.embed.process.runtime.Network
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager
import org.jboss.logging.Logger

class MongoTestResource : QuarkusTestResourceLifecycleManager {
    companion object {
        private val LOGGER: Logger = Logger.getLogger(MongoTestResource::class.java)
    }

    private var mongod: MongodExecutable? = null

    override fun start(): Map<String, String> {
        try {
            //JDK bug workaround
            //https://github.com/quarkusio/quarkus/issues/14424
            //force class init to prevent possible deadlock when done by mongo threads
            Class.forName("sun.net.ext.ExtendedSocketOptions", true, ClassLoader.getSystemClassLoader());
        } catch (e: ClassNotFoundException) {
        }
        return try {
            val version: Version.Main = Version.Main.V4_0
            val port = 27018
            LOGGER.infof("Starting Mongo %s on port %s", version, port)
            val config: IMongodConfig = MongodConfigBuilder()
                    .version(version)
                    .net(Net(port, Network.localhostIsIPv6()))
                    .build()
            mongod = getMongodExecutable(config).also {
                try {
                    it.start()
                } catch (e: Exception) {
                    //every so often mongo fails to start on CI runs
                    //see if this helps
                    Thread.sleep(1000)
                    it.start()
                }

            }
            mapOf()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    private fun getMongodExecutable(config: IMongodConfig): MongodExecutable {
        return try {
            doGetExecutable(config)
        } catch (e: Exception) {
            // sometimes the download process can timeout so just sleep and try again
            try {
                Thread.sleep(1000)
            } catch (ignored: InterruptedException) {
            }
            doGetExecutable(config)
        }
    }

    private fun doGetExecutable(config: IMongodConfig): MongodExecutable {
        val runtimeConfig = RuntimeConfigBuilder()
                .defaults(Command.MongoD)
                .processOutput(ProcessOutput(Processors.silent(),
                        Processors.silent(), Processors.silent()))
                .build()
        return MongodStarter.getInstance(runtimeConfig).prepare(config)
    }

    override fun stop() {
        mongod?.stop()
    }
}
