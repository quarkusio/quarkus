package org.acme.resteasyjackson

import java.util.*
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Path("/resteasy-jackson/quarks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class JacksonResource {

    private val quarks = Collections.newSetFromMap(Collections.synchronizedMap(LinkedHashMap<Quark, Boolean>()))

    init {
        quarks.add(Quark("Up", "The up quark or u quark (symbol: u) is the lightest of all quarks, a type of elementary particle, and a major constituent of matter."))
        quarks.add(Quark("Strange", "The strange quark or s quark (from its symbol, s) is the third lightest of all quarks, a type of elementary particle."))
        quarks.add(Quark("Charm", "The charm quark, charmed quark or c quark (from its symbol, c) is the third most massive of all quarks, a type of elementary particle."))
        quarks.add(Quark("???", null))
    }

    @GET
    fun list(): Set<Quark> {
        return quarks
    }

    @POST
    fun add(quark: Quark): Set<Quark> {
        quarks.add(quark)
        return quarks
    }

    @DELETE
    fun delete(quark: Quark): Set<Quark> {
        quarks.removeIf { existingQuark: Quark -> existingQuark.name!!.contentEquals(quark.name!!) }
        return quarks
    }

    class Quark {
        var name: String? = null
        var description: String? = null

        constructor() {}
        constructor(name: String?, description: String?) {
            this.name = name
            this.description = description
        }
    }
}
