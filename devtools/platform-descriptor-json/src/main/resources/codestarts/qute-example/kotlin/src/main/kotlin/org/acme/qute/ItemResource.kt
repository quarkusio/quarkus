package org.acme.qute

import io.quarkus.qute.Template
import io.quarkus.qute.TemplateExtension
import io.quarkus.qute.TemplateInstance
import java.math.BigDecimal
import java.util.*
import javax.inject.Inject
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType


@Path("qute/items")
class ItemResource {

    @Inject
    lateinit var items: Template

    @GET
    @Produces(MediaType.TEXT_HTML)
    fun get(): TemplateInstance {
        val data: MutableList<Item> = ArrayList()
        data.add(Item(BigDecimal(10), "Apple"))
        data.add(Item(BigDecimal(16), "Pear"))
        data.add(Item(BigDecimal(30), "Orange"))
        return items.data("items", data)
    }
}

@TemplateExtension
class ItemExtension {

    companion object {
        /**
         * This template extension method implements the "discountedPrice" computed property.
         */
        @JvmStatic
        fun discountedPrice(item: Item): BigDecimal {
            return item.price.multiply(BigDecimal("0.9"))
        }
    }
}