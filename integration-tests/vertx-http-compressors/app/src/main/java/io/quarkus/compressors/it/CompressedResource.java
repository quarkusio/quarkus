package io.quarkus.compressors.it;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.quarkus.vertx.http.Compressed;
import io.quarkus.vertx.http.Uncompressed;

public class CompressedResource {

    // If you touch this block of text, you need to adjust byte numbers in RESTEndpointsTest too.
    // The text is not entirely arbitrary, it shows different compress ratios for
    // Brotli and GZip while still being reasonably short.
    public static final String TEXT = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor "
            + "incididunt ut labore et <SOME TAG></SOME TAG> <SOME TAG></SOME TAG> minim veniam, quis nostrud exercitation"
            + "dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip "
            + "ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu "
            + "fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt "
            + "mollit anim id est laborum. consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et "
            + "dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip "
            + "ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu "
            + "fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt "
            + "mollit anim id est laborum. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia XXX "
            + "❀ੈ✩‧₊˚✧˚༘ ☹#️©️®️‼️⁉️™️ℹ️↔️↕️↖️↗️↘️↙️↩️↪️⌚⌛⌨️⏏️⏩⏬⏭️⏮️⏯️⏰⏱️⏲️⏳⏸️⏹️⏺️Ⓜ️▪️▫️▶️◀️◻️◼️◽◾☀️☁️☂️☃️☄️☎️☑️☔☕☘️☝️☝☝☝☝☝☠️☢️☣️☦️☪️"
            + "☮️☯️☸️☹️☺️♀️♂️♈♓♟️♠️♣️♥️♦️♨️♻️♾️♿⚒️⚓⚔️⚕️⚖️⚗️⚙️⚛️⚜️⚠️⚡⚧️⚪⚫⚰️⚱️⚽⚾⛄⛅⛈️⛎⛏️⛑️⛓️⛔⛩️⛪⛰️⛱️⛲⛳⛴️⛵⛷️⛸️⛹️⛹⛹⛹⛹⛹⛺"
            + "⛽✂️✅✈️✉️✊✋✊✊✊✊✊✋✋✋✋✋✌️✌✌✌✌✌✍️✍✍✍✍✍✏️✒️✔️✖️✝️✡️✨✳️✴️❄️❇️❌❎❓❕❗❣️❤️➕➗➡️➰➿⤴️⤵️⬅️⬆️⬇️⬛⬜⭐"
            + "⭕〰️〽️㊗️㊙️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️️⋆｡♡˚✧˚ ༘ ⋆｡♡˚ consectetur adipiscing elit. Phasellus interdum erat ligula, eget consectetur consect"
            + "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Phasellus interdum erat ligula, eget consectetur justo"
            + "porttitor nec. Sed at est auctor, suscipit erat at, volutpat purus. Nulla facilisi. Praesent suscipit purus vel"
            + "nisl pharetra, in sagittis neque tincidunt. Curabitur sit amet ligula nec nisi mollis vehicula. Morbi sit amet "
            + "magna vitae arcu bibendum interdum. Vestibulum luctus felis sed tellus egestas, non suscipit risus dignissim. "
            + "Integer auctor tincidunt purus, ac posuere massa tristique id. Fusce varius eu ex ut cursus. Vestibulum vehicula"
            + "purus ut orci fermentum, ut feugiat dui fringilla. Ut in turpis at odio bibendum lacinia. 4231032147093284721037"
            + "Duis ultrices semper velit ut varius. Nam porttitor magna sed dui vestibulum, nec bibendum tortor convallis. 666"
            + "Pellentesque habitant morbi tristique senectus et netus et malesuada fames ac turpis egestas. In at augue quis "
            + "justo aliquet aliquet ut non eros. Vestibulum finibus ligula magna, at euismod tortor efficitur sit amet. Aliquam"
            + "erat volutpat. Curabitur bibendum orci vel risus fermentum, a gravida nulla placerat. Morbi condimentum dolor et "
            + "ex finibus, et finibus magna congue. Integer et odio ut sapien aliquam pharetra. Curabitur pharetra urna id felis"
            + "fringilla tincidunt. Suspendisse a erat quis enim pharetra mollis. Fusce vel est non odio tincidunt vulputate a "
            + "nec sem. Donec finibus sapien sed purus tincidunt, ac venenatis felis hendrerit. Ut consectetur lacus vel urna "
            + "suscipit, sed laoreet nulla volutpat.\n\r\n\r\n\r\n\t\t\t\n\r\n\r\n\r\n\r\n\t\t\t\n\r\n\r\n\r\n\r\n\t\t\t\n\r\r\r"
            + "Pellentesque habitant morbi tristique senectus et netus et malesuada fames ac turpis egestas. Donec consequat "
            + "purus a odio gravida, vel convallis justo volutpat. Integer in magna a ante porttitor ultricies at id dui. Nullam "
            + "elementum sapien ut magna posuere suscipit. Proin aliquam dolor et quam suscipit bibendum. Aenean suscipit velit "
            + "vel lectus posuere, ut convallis nulla consequat. In at lorem fermentum, dignissim nulla ut, consectetur enim. "
            + "Maecenas vestibulum felis id justo blandit suscipit. Nulla facilisi. Duis elementum orci nec sapien accumsan, eget "
            + "tempus turpis rhoncus. Quisque porttitor, mi in auctor fermentum, mi orci hendrerit risus, in dignissim erat nunc "
            + "Morbi faucibus quam vel libero pharetra, non ultricies felis laoreet. Duis vulputate purus id sem interdum, vel "
            + "risus gravida. Vestibulum vulputate purus non lorem ultricies varius. Donec bibendum libero a mi sollicitudin \n"
            + "Praesent sed diam nec nunc pharetra malesuada nec ac urna. Nam id dui id eros vulputate pellentesque. Nulla malesuada e"
            + "ros eu enim finibus, sit amet posuere leo condimentum. Curabitur in mauris lacus. Aenean nec enim ut elit bibendum suscipit ac "
            + "nec nunc. Vestibulum vitae libero ac ipsum faucibus scelerisque. Curabitur ut lorem feugiat, pellentesque risus sit amet, "
            + "vehicula metus. Quisque vulputate arcu et magna vehicula pharetra. Integer gravida diam et dolor hendrerit, nec suscipit odio "
            + "vehicula. Donec sit amet turpis ut nulla viverra pharetra. Aenean commodo nisl ut risus fermentum vehicula. Suspendisse "
            + "at augue in lorem suscipit venenatis. Morbi interdum nibh eget ex posuere, sed convallis ipsum pharetra. Aliquam auctor "
            + "tincidunt urna, non dapibus risus fermentum ut. Phasellus convallis ipsum a diam consectetur, sit amet posuere erat viverra.\n"
            + "Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia curae; Quisque non orci scelerisque, "
            + "dapibus ipsum nec, laoreet magna. Suspendisse eget bibendum dui. Aenean bibendum augue vel risus laoreet faucibus. Donec "
            + "quis massa sapien. Nulla facilisi. Nulla facilisi. Nullam vel varius ipsum. Cras a tincidunt libero. Fusce efficitur felis "
            + "et pharetra cursus. Nullam venenatis mi nec libero auctor, nec cursus ligula dignissim. Phasellus eget libero consequat, "
            + "elementum erat sed, viverra odio. Suspendisse potenti. Aenean feugiat mi a tincidunt tristique. Sed fringilla magna nec "
            + "magna ultricies, id bibendum lacus faucibus. Maecenas porttitor libero sed lacus efficitur bibendum. Proin eget felis "
            + "dictum, malesuada ipsum ac, finibus nisl. ipsum ac, finibus nisl. 1234567890 148130 01394829387293 932583275295 2938573592\n";

    @GET
    @Path("/yes/text")
    @Produces(MediaType.TEXT_PLAIN)
    @Compressed
    public Response textCompressed() {
        return Response.ok(TEXT).build();
    }

    @GET
    @Path("/no/text")
    @Produces(MediaType.TEXT_PLAIN)
    @Uncompressed
    public String textUncompressed() {
        return TEXT;
    }

    @GET
    @Path("/yes/json")
    @Produces(MediaType.APPLICATION_JSON)
    @Compressed
    public String jsonCompressed() {
        // Doesn't matter the text ain't JSON, the test doesn't care.
        // We need just the correct header.
        return TEXT;
    }

    @GET
    @Path("/no/json")
    @Produces(MediaType.APPLICATION_JSON)
    @Uncompressed
    public String jsonUncompressed() {
        return TEXT;
    }

    @GET
    @Path("/yes/xml")
    @Produces(MediaType.TEXT_XML)
    // This one is compressed via default quarkus.http.compress-media-types
    public String xmlCompressed() {
        // Doesn't matter the text ain't XML, the test doesn't care.
        // We need just the correct header.
        return TEXT;
    }

    @GET
    @Path("/no/xml")
    @Produces(MediaType.TEXT_XML)
    @Uncompressed
    public String xmlUncompressed() {
        return TEXT;
    }

    @GET
    @Path("/yes/xhtml")
    @Produces(MediaType.APPLICATION_XHTML_XML)
    // This one is compressed quarkus.http.compress-media-types setting
    public String xhtmlCompressed() {
        // Doesn't matter the text ain't XML, the test doesn't care.
        // We need just the correct header.
        return TEXT;
    }
}
