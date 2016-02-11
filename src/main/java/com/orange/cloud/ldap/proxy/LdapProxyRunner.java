package com.orange.cloud.ldap.proxy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Controller;

import java.util.List;

/**
 * Copyright (C) 2016 Orange
 * <p>
 * This software is distributed under the terms and conditions of the 'Apache-2.0'
 * license which can be found in the file 'LICENSE' in this package distribution
 * or at 'https://opensource.org/licenses/Apache-2.0'.
 * <p>
 * Author: Arthur Halet
 * Date: 10/02/2016
 */
@Controller
public class LdapProxyRunner implements CommandLineRunner {

    @Value("#{'${ldap.remote.adresses}'.split(',')}")
    private List<String> ldapRemoteAddresses;

    @Value("${ldap.proxy.password:password}")
    private String ldapProxyPassword;

    @Value("${ldap.proxy.dn:dn}")
    private String ldapProxyDn;

    @Value("${HOST:0.0.0.0}")
    private String ldapProxyHost;

    @Value("${PORT:8080}")
    private String ldapProxyPort;

    @Override
    public void run(String... strings) throws Exception {
        LdapProxy ldapProxy = new LdapProxy(this.ldapRemoteAddresses, this.ldapProxyPassword, this.ldapProxyDn, this.ldapProxyHost, this.ldapProxyPort);
        ldapProxy.runServer();
    }
}
