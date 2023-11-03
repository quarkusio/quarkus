FROM ${keycloak.docker.image} as builder

COPY ./*.jar /opt/keycloak/providers/

FROM ${keycloak.docker.image}
COPY --from=builder /opt/keycloak/ /opt/keycloak/

ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
