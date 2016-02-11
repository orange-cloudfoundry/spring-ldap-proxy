# spring-ldap-proxy

Proxy LDAP runnable in a spring-boot environment

## Env var to be set (or spring var)

- **HOST** (Optional): Address to listen - Default: `0.0.0.0`
- **PORT** (Optional): Port to listen - Default: `8080`
- **ldap_proxy_password** (Optional): LDAP proxy password - Default: `password`
- **ldap_proxy_dn**: Dn for proxy ldap
- **ldap_remote_adresses**: Remote LDAPs to proxify (list is separated with `,`) - Example: `127.0.0.1:380,192.168.0.2:380`

## Run in docker

```
docker run -d \
  --name spring-ldap-proxy \
  -h spring-ldap-proxy \
  -p 3801:380 \
  -e ldap_proxy_dn=myowndn \
  -e ldap_proxy_password=mysuperpassword \
  -e ldap_remote_adresses=127.0.0.1:380,192.168.0.2:380 \
  arthurhlt/spring-ldap-proxy
```
