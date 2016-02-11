package com.orange.cloud.ldap.proxy;

import org.forgerock.opendj.ldap.*;
import org.forgerock.opendj.ldap.controls.ProxiedAuthV2RequestControl;
import org.forgerock.opendj.ldap.requests.*;
import org.forgerock.opendj.ldap.responses.BindResult;
import org.forgerock.opendj.ldap.responses.CompareResult;
import org.forgerock.opendj.ldap.responses.ExtendedResult;
import org.forgerock.opendj.ldap.responses.Result;
import org.forgerock.util.AsyncFunction;
import org.forgerock.util.Utils;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.ResultHandler;

import java.io.Closeable;
import java.util.concurrent.atomic.AtomicReference;

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
final class ProxyBackend implements RequestHandler<RequestContext> {
    private final ConnectionFactory bindFactory;
    private final ConnectionFactory factory;
    private volatile ProxiedAuthV2RequestControl proxiedAuthControl;

    ProxyBackend(ConnectionFactory factory, ConnectionFactory bindFactory) {
        this.factory = factory;
        this.bindFactory = bindFactory;
    }

    public void handleAdd(RequestContext requestContext, final AddRequest request, final IntermediateResponseHandler intermediateResponseHandler, LdapResultHandler<Result> resultHandler) {
        final AtomicReference connectionHolder = new AtomicReference();
        this.addProxiedAuthControl(request);
        this.factory.getConnectionAsync().thenAsync(new AsyncFunction<Connection, Result, LdapException>() {

            public Promise<Result, LdapException> apply(Connection connection) throws LdapException {
                connectionHolder.set(connection);
                return connection.addAsync(request, intermediateResponseHandler);
            }

        }).thenOnResult(resultHandler).thenOnException(resultHandler).thenAlways(this.close(connectionHolder));
    }

    public void handleBind(RequestContext requestContext, int version, final BindRequest request, final IntermediateResponseHandler intermediateResponseHandler, final LdapResultHandler<BindResult> resultHandler) {
        if (request.getAuthenticationType() != -128) {
            resultHandler.handleException(LdapException.newLdapException(ResultCode.PROTOCOL_ERROR, "non-SIMPLE authentication not supported: " + request.getAuthenticationType()));
        } else {
            final AtomicReference connectionHolder = new AtomicReference();
            this.proxiedAuthControl = null;
            this.bindFactory.getConnectionAsync().thenAsync(new AsyncFunction<Connection, BindResult, LdapException>() {
                public Promise<BindResult, LdapException> apply(Connection connection) throws LdapException {
                    connectionHolder.set(connection);
                    return connection.bindAsync(request, intermediateResponseHandler);
                }
            }).thenOnResult(new ResultHandler<BindResult>() {
                public final void handleResult(BindResult result) {
                    ProxyBackend.this.proxiedAuthControl = ProxiedAuthV2RequestControl.newControl("dn:" + request.getName());
                    resultHandler.handleResult(result);
                }
            }).thenOnException(resultHandler).thenAlways(this.close(connectionHolder));
        }

    }

    public void handleCompare(RequestContext requestContext, final CompareRequest request, final IntermediateResponseHandler intermediateResponseHandler, LdapResultHandler<CompareResult> resultHandler) {
        this.addProxiedAuthControl(request);
        final AtomicReference connectionHolder = new AtomicReference();
        this.factory.getConnectionAsync().thenAsync(new AsyncFunction<Connection, CompareResult, LdapException>() {
            public Promise<CompareResult, LdapException> apply(Connection connection) throws LdapException {
                connectionHolder.set(connection);
                return connection.compareAsync(request, intermediateResponseHandler);
            }
        }).thenOnResult(resultHandler).thenOnException(resultHandler).thenAlways(this.close(connectionHolder));
    }

    public void handleDelete(RequestContext requestContext, final DeleteRequest request, final IntermediateResponseHandler intermediateResponseHandler, LdapResultHandler<Result> resultHandler) {
        this.addProxiedAuthControl(request);
        final AtomicReference connectionHolder = new AtomicReference();
        this.factory.getConnectionAsync().thenAsync(new AsyncFunction<Connection, Result, LdapException>() {
            public Promise<Result, LdapException> apply(Connection connection) throws LdapException {
                connectionHolder.set(connection);
                return connection.deleteAsync(request, intermediateResponseHandler);
            }
        }).thenOnResult(resultHandler).thenOnException(resultHandler).thenAlways(this.close(connectionHolder));
    }

    public <R extends ExtendedResult> void handleExtendedRequest(RequestContext requestContext, final ExtendedRequest<R> request, final IntermediateResponseHandler intermediateResponseHandler, LdapResultHandler<R> resultHandler) {
        if ("1.3.6.1.1.8".equals(request.getOID())) {
            resultHandler.handleException(LdapException.newLdapException(ResultCode.PROTOCOL_ERROR, "Cancel extended request operation not supported"));
        } else if ("1.3.6.1.4.1.1466.20037".equals(request.getOID())) {
            resultHandler.handleException(LdapException.newLdapException(ResultCode.PROTOCOL_ERROR, "StartTLS extended request operation not supported"));
        } else {
            this.addProxiedAuthControl(request);
            final AtomicReference connectionHolder = new AtomicReference();
            this.factory.getConnectionAsync().thenAsync(new AsyncFunction<Connection, R, LdapException>() {
                public Promise<R, LdapException> apply(Connection connection) throws LdapException {
                    connectionHolder.set(connection);
                    return connection.extendedRequestAsync(request, intermediateResponseHandler);
                }
            }).thenOnResult(resultHandler).thenOnException(resultHandler).thenAlways(this.close(connectionHolder));
        }

    }

    public void handleModify(RequestContext requestContext, final ModifyRequest request, final IntermediateResponseHandler intermediateResponseHandler, LdapResultHandler<Result> resultHandler) {
        this.addProxiedAuthControl(request);
        final AtomicReference connectionHolder = new AtomicReference();
        this.factory.getConnectionAsync().thenAsync(new AsyncFunction<Connection, Result, LdapException>() {
            public Promise<Result, LdapException> apply(Connection connection) throws LdapException {
                connectionHolder.set(connection);
                return connection.modifyAsync(request, intermediateResponseHandler);
            }
        }).thenOnResult(resultHandler).thenOnException(resultHandler).thenAlways(this.close(connectionHolder));
    }

    public void handleModifyDN(RequestContext requestContext, final ModifyDNRequest request, final IntermediateResponseHandler intermediateResponseHandler, LdapResultHandler<Result> resultHandler) {
        this.addProxiedAuthControl(request);
        final AtomicReference connectionHolder = new AtomicReference();
        this.factory.getConnectionAsync().thenAsync(new AsyncFunction<Connection, Result, LdapException>() {
            public Promise<Result, LdapException> apply(Connection connection) throws LdapException {
                connectionHolder.set(connection);
                return connection.modifyDNAsync(request, intermediateResponseHandler);
            }
        }).thenOnResult(resultHandler).thenOnException(resultHandler).thenAlways(this.close(connectionHolder));
    }

    public void handleSearch(RequestContext requestContext, final SearchRequest request, final IntermediateResponseHandler intermediateResponseHandler, final SearchResultHandler entryHandler, LdapResultHandler<Result> resultHandler) {
        this.addProxiedAuthControl(request);
        final AtomicReference connectionHolder = new AtomicReference();
        this.factory.getConnectionAsync().thenAsync(new AsyncFunction<Connection, Result, LdapException>() {
            public Promise<Result, LdapException> apply(Connection connection) throws LdapException {
                connectionHolder.set(connection);
                return connection.searchAsync(request, intermediateResponseHandler, entryHandler);
            }
        }).thenOnResult(resultHandler).thenOnException(resultHandler).thenAlways(this.close(connectionHolder));
    }

    private void addProxiedAuthControl(Request request) {
        ProxiedAuthV2RequestControl control = this.proxiedAuthControl;
        if (control != null) {
            request.addControl(control);
        }

    }

    private Runnable close(final AtomicReference<Connection> c) {
        return new Runnable() {
            public void run() {
                Utils.closeSilently(new Closeable[]{(Closeable) c.get()});
            }
        };
    }
}
