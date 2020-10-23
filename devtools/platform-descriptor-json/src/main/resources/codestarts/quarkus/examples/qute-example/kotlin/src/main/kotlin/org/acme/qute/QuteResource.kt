package org.acme.qute

import io.quarkus.qute.Template
import io.quarkus.qute.TemplateExtension
import io.quarkus.qute.TemplateInstance
import java.util.*
import javax.inject.Inject
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/qute/quarks")
class QuteResource {
    private val quarks = Collections.synchronizedList(ArrayList<Quark>())

    init {
        for (i in 0..2) {
            addQuark()
        }
    }

    @Inject
    lateinit var page: Template

    @GET
    @Produces(MediaType.TEXT_HTML)
    fun get(): TemplateInstance {
        return page.data("quarks", ArrayList(quarks))
    }

    @POST
    @Path("add")
    fun addQuark() {
        val random = Random()
        val flavor = Quark.Flavor.values()[random.nextInt(Quark.Flavor.values().size)]
        val color = Quark.Color.values()[random.nextInt(Quark.Color.values().size)]
        quarks.add(Quark(flavor, color))
    }

    @TemplateExtension
    class QuarkExtension {
        companion object {
            /**
             * This template extension method implements the "position" computed property.
             */
            @JvmStatic
            fun position(quark: Quark): Int {
                return Random().nextInt(100)
            }
        }
    }
}
