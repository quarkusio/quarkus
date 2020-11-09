package org.acme.config

import org.eclipse.microprofile.config.inject.ConfigProperty
import java.math.BigDecimal
import java.util.*
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/config")
class ConfigResource {
    @ConfigProperty(name = "constant.speed-of-sound-in-meter-per-second", defaultValue = "343")
    var speedOfSound = 0

    @ConfigProperty(name = "display.mach")
    lateinit var displayMach: Optional<Int>

    @ConfigProperty(name = "display.unit.name")
    lateinit var displayUnitName: String

    @ConfigProperty(name = "display.unit.factor")
    lateinit var displayUnitFactor: BigDecimal

    @GET
    @Path("supersonic")
    @Produces(MediaType.TEXT_PLAIN)
    fun supersonic(): String {
        val mach = displayMach.orElse(1)
        val speed = BigDecimal.valueOf(speedOfSound.toLong())
                .multiply(displayUnitFactor)
                .multiply(BigDecimal.valueOf(mach.toLong()))
        return String.format("Mach %d is %.3f %s",
                mach,
                speed,
                displayUnitName
        )
    }
}
