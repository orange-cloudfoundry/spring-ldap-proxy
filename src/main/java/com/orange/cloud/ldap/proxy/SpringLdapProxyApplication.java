package com.orange.cloud.ldap.proxy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

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
@SpringBootApplication
public class SpringLdapProxyApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringLdapProxyApplication.class, args);
    }
}
