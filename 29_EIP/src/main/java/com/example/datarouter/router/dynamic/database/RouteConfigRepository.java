package com.example.datarouter.router.dynamic.database;

public interface RouteConfigRepository  {

    String findByHeaderName(String headerName);
}
