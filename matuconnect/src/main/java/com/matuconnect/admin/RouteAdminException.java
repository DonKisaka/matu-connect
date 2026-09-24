package com.matuconnect.admin;

/** A route-editing request that fails validation — always the caller's fault (400), never a server error. */
public class RouteAdminException extends RuntimeException {
    public RouteAdminException(String message) {
        super(message);
    }
}
