package io.quarkus.vault.test.client.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.vault.runtime.client.dto.VaultModel;

/*

{
    "keys":["1d86d9e6b46d49e67ded5b2d18098be3cba621d2f6434bc7e1d2384a35149aaf"],
    "keys_base64":["HYbZ5rRtSeZ97VstGAmL48umIdL2Q0vH4dI4SjUUmq8="],
    "root_token":"s.NB8TDce8Teqqa8Uv0qxfQXSZ"
}

*/
public class VaultInit implements VaultModel {

    public List<String> keys;
    @JsonProperty("keys_base64")
    public List<String> keysBase64;
    @JsonProperty("root_token")
    public String rootToken;

}
