package com.orange.cloud.ldap.proxy;

/**
 * Copyright (C) 2016 Arthur Halet
 * <p>
 * This software is distributed under the terms and conditions of the 'MIT'
 * license which can be found in the file 'LICENSE' in this package distribution
 * or at 'http://opensource.org/licenses/MIT'.
 * <p>
 * Author: Arthur Halet
 * Date: 10/02/2016
 */

import org.forgerock.opendj.ldap.*;
import org.forgerock.opendj.ldap.requests.BindRequest;
import org.forgerock.util.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import static org.forgerock.opendj.ldap.LDAPConnectionFactory.AUTHN_BIND_REQUEST;
import static org.forgerock.opendj.ldap.LDAPConnectionFactory.HEARTBEAT_ENABLED;
import static org.forgerock.opendj.ldap.LDAPListener.CONNECT_MAX_BACKLOG;
import static org.forgerock.opendj.ldap.requests.Requests.newSimpleBindRequest;

/**
 * An LDAP load balancing proxy which forwards requests to one or more remote
 * Directory Servers. This is implementation is very simple and is only intended
 * as an example:
 * <ul>
 * <li>It does not support SSL connections
 * <li>It does not support StartTLS
 * <li>It does not support Abandon or Cancel requests
 * <li>Very basic authentication and authorization support.
 * </ul>
 * This example takes the following command line parameters:
 * <p>
 * <pre>
 *     {@code <listenAddress> <listenPort> <proxyDN> <proxyPassword> <remoteAddress1> <remotePort1>
 *      [<remoteAddress2> <remotePort2> ...]}
 * </pre>
 */
public class LdapProxy {
    private Logger logger = LoggerFactory.getLogger(LdapProxy.class);
    private List<String> ldapRemoteAddresses;

    private String ldapProxyPassword;

    private String ldapProxyDn;

    private String ldapProxyHost;

    private String ldapProxyPort;

    private List<ConnectionFactory> factories;
    private List<ConnectionFactory> bindFactories;
    private ConnectionFactory factory;
    private ConnectionFactory bindFactory;

    public LdapProxy(List<String> ldapRemoteAddresses, String ldapProxyPassword, String ldapProxyDn, String ldapProxyHost, String ldapProxyPort) {
        this.ldapRemoteAddresses = ldapRemoteAddresses;
        this.ldapProxyPassword = ldapProxyPassword;
        this.ldapProxyDn = ldapProxyDn;
        this.ldapProxyHost = ldapProxyHost;
        this.ldapProxyPort = ldapProxyPort;
    }

    public void runServer() {
        this.factories = new LinkedList<>();
        this.bindFactories = new LinkedList<>();
        this.loadProxies();
        /*
         * Create a server connection adapter which will create a new proxy
         * backend for each inbound client connection. This is required because
         * we need to maintain authorization state between client requests.
         */
        RequestHandlerFactory<LDAPClientContext, RequestContext> proxyFactory =
                new RequestHandlerFactory<LDAPClientContext, RequestContext>() {
                    @Override
                    public ProxyBackend handleAccept(LDAPClientContext clientContext)
                            throws LdapException {
                        return new ProxyBackend(factory, bindFactory);
                    }
                };
        ServerConnectionFactory<LDAPClientContext, Integer> connectionHandler =
                Connections.newServerConnectionFactory(proxyFactory);
        // --- JCite backend ---

        // --- JCite listener ---
        // Create listener.
        Options options = Options.defaultOptions().set(CONNECT_MAX_BACKLOG, 4096);
        LDAPListener listener = null;
        try {
            listener = new LDAPListener(this.ldapProxyHost, Integer.parseInt(this.ldapProxyPort), connectionHandler, options);
            System.out.println("Start listening on " + this.ldapProxyHost + ":" + this.ldapProxyPort);
            System.out.println("Press any key to stop the server...");
            System.in.read();
        } catch (final IOException e) {
            System.out.println("Error listening on " + this.ldapProxyHost + ":" + this.ldapProxyPort);
            e.printStackTrace();
        } finally {
            if (listener != null) {
                listener.close();
            }
        }
    }

    private void loadProxies() {
        BindRequest bindRequest = newSimpleBindRequest(this.ldapProxyDn, this.ldapProxyPassword.toCharArray());
        Options factoryOptions = Options.defaultOptions()
                .set(HEARTBEAT_ENABLED, true)
                .set(AUTHN_BIND_REQUEST, bindRequest);

        Options bindFactoryOptions = Options.defaultOptions().set(HEARTBEAT_ENABLED, true);

        for (String address : this.ldapRemoteAddresses) {
            String[] adressSplit = address.split(Pattern.quote(":"));
            String remoteAddress = adressSplit[0];
            int remotePort = Integer.parseInt(adressSplit[1]);
            logger.info("Registering remote ldap server '" + remoteAddress + "' with port " + remotePort + ".");
            this.factories.add(Connections.newCachedConnectionPool(new LDAPConnectionFactory(remoteAddress,
                    remotePort,
                    factoryOptions)));

            this.bindFactories.add(Connections.newCachedConnectionPool(new LDAPConnectionFactory(remoteAddress,
                    remotePort,
                    bindFactoryOptions)));
        }
        // --- JCite load balancer ---
        this.factory = Connections.newRoundRobinLoadBalancer(factories, factoryOptions);
        this.bindFactory = Connections.newRoundRobinLoadBalancer(bindFactories, bindFactoryOptions);
        // --- JCite load balancer ---
    }
}

